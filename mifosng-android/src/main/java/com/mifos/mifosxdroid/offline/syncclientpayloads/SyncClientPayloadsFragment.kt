package com.mifos.mifosxdroid.offline.syncclientpayloads

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.mifos.mifosxdroid.R
import com.mifos.mifosxdroid.adapters.SyncPayloadsAdapter
import com.mifos.mifosxdroid.core.MaterialDialog
import com.mifos.mifosxdroid.core.MifosBaseActivity
import com.mifos.mifosxdroid.core.MifosBaseFragment
import com.mifos.mifosxdroid.core.util.Toaster.show
import com.mifos.mifosxdroid.databinding.FragmentSyncpayloadBinding
import com.mifos.objects.client.ClientPayload
import com.mifos.utils.Constants
import com.mifos.utils.PrefManager.userStatus
import javax.inject.Inject

/**
 * This Class for Syncing the clients that is created in offline mode.
 * For syncing the clients user make sure that he/she is in the online mode.
 *
 *
 * Created by Rajan Maurya on 08/07/16.
 */
class SyncClientPayloadsFragment : MifosBaseFragment(), SyncClientPayloadsMvpView,
    DialogInterface.OnClickListener {
    val LOG_TAG = javaClass.simpleName

    private lateinit var binding: FragmentSyncpayloadBinding

    @Inject
    lateinit var mSyncPayloadsPresenter: SyncClientPayloadsPresenter
    var clientPayloads: MutableList<ClientPayload>? = null
    var mSyncPayloadsAdapter: SyncPayloadsAdapter? = null
    var mClientSyncIndex = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as MifosBaseActivity).activityComponent?.inject(this)
        clientPayloads = ArrayList()
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSyncpayloadBinding.inflate(inflater, container, false)
        mSyncPayloadsPresenter.attachView(this)
        val mLayoutManager = LinearLayoutManager(activity)
        mLayoutManager.orientation = LinearLayoutManager.VERTICAL
        binding.rvSyncPayload.layoutManager = mLayoutManager
        binding.rvSyncPayload.setHasFixedSize(true)
        mSyncPayloadsPresenter.loadDatabaseClientPayload()
        /**
         * Loading All Client Payloads from Database
         */
        binding.swipeContainer.setColorSchemeColors(
            *requireActivity().resources.getIntArray(R.array.swipeRefreshColors)
        )
        binding.swipeContainer.setOnRefreshListener {
            mSyncPayloadsPresenter.loadDatabaseClientPayload()
            if (binding.swipeContainer.isRefreshing) binding.swipeContainer.isRefreshing = false
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.noPayloadIcon.setOnClickListener {
            reloadOnError()
        }
    }

    /**
     * Show when Database response is null or failed to fetch the client payload
     * Onclick Send Fresh Request for Client Payload.
     */

    private fun reloadOnError() {
        binding.llError.visibility = View.GONE
        mSyncPayloadsPresenter.loadDatabaseClientPayload()
    }

    /**
     * This method is showing the client payload in the recyclerView.
     * If Database Table have no entry then showing make recyclerView visibility gone and
     * visible to the noPayloadIcon and noPayloadText to alert the user there is nothing
     * to show.
     *
     * @param clientPayload
     */
    override fun showPayloads(clientPayload: List<ClientPayload>) {
        clientPayloads = clientPayload as MutableList<ClientPayload>
        if (clientPayload.isEmpty()) {
            binding.llError.visibility = View.VISIBLE
            binding.noPayloadText.text = requireActivity()
                .resources.getString(R.string.no_client_payload_to_sync)
            binding.noPayloadIcon.setImageResource(R.drawable.ic_assignment_turned_in_black_24dp)
        } else {
            mSyncPayloadsAdapter = SyncPayloadsAdapter(requireActivity(), clientPayload)
            binding.rvSyncPayload.adapter = mSyncPayloadsAdapter
        }
    }

    /**
     * Showing Error when failed to fetch client payload from Database
     *
     * @param s Error String
     */
    override fun showError(stringId: Int) {
        binding.llError.visibility = View.VISIBLE
        val message =
            stringId.toString() + activity!!.resources.getString(R.string.click_to_refresh)
        binding.noPayloadText.text = message
        show(binding.root, resources.getString(stringId))
    }

    /**
     * Called Whenever any client payload is synced to server.
     * then first delete that client from database and sync again from Database
     * and update the recyclerView
     */
    override fun showSyncResponse() {
        clientPayloads?.get(mClientSyncIndex)?.id?.let {
            clientPayloads?.get(mClientSyncIndex)?.clientCreationTime?.let { it1 ->
                mSyncPayloadsPresenter.deleteAndUpdateClientPayload(
                    it,
                    it1
                )
            }
        }
    }

    /**
     * Called when client payload synced is failed then there can be some problem with the
     * client payload data example externalId or phone number already exist.
     * If client synced failed the there is no need to delete from the Database and increase
     * the mClientSyncIndex by one and sync the next client payload
     */
    override fun showClientSyncFailed(errorMessage: String) {
        val clientPayload = clientPayloads!![mClientSyncIndex]
        clientPayload.errorMessage = errorMessage
        mSyncPayloadsPresenter.updateClientPayload(clientPayload)
    }

    /**
     * This Method will called whenever user trying to sync the client payload in
     * offline mode.
     */
    override fun showOfflineModeDialog() {
        MaterialDialog.Builder().init(activity)
            .setTitle(R.string.offline_mode)
            .setMessage(R.string.dialog_message_offline_sync_alert)
            .setPositiveButton(R.string.dialog_action_go_online, this)
            .setNegativeButton(R.string.dialog_action_cancel, this)
            .createMaterialDialog()
            .show()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        when (which) {
            DialogInterface.BUTTON_NEGATIVE -> {}
            DialogInterface.BUTTON_POSITIVE -> {
                userStatus = Constants.USER_ONLINE
                if (clientPayloads!!.isNotEmpty()) {
                    mClientSyncIndex = 0
                    syncClientPayload()
                } else {
                    show(
                        binding.root,
                        activity!!.resources.getString(R.string.nothing_to_sync)
                    )
                }
            }

            else -> {}
        }
    }

    /**
     * This method will update client Payload in List<ClientPayload> after adding Error message in
     * database
     *
     * @param clientPayload
    </ClientPayload> */
    override fun showClientPayloadUpdated(clientPayload: ClientPayload) {
        clientPayloads?.set(mClientSyncIndex, clientPayload)
        mSyncPayloadsAdapter!!.notifyDataSetChanged()
        mClientSyncIndex += 1
        if (clientPayloads!!.size != mClientSyncIndex) {
            syncClientPayload()
        }
    }

    /**
     * This is called whenever a client  payload is synced and synced client payload is
     * deleted from the Database and update UI
     *
     * @param clients
     */
    override fun showPayloadDeletedAndUpdatePayloads(clients: List<ClientPayload>) {
        mClientSyncIndex = 0
        clientPayloads?.clear()
        clientPayloads = clients as MutableList<ClientPayload>
        mSyncPayloadsAdapter!!.setClientPayload(clientPayloads!!)
        if (clientPayloads!!.isNotEmpty()) {
            syncClientPayload()
        } else {
            binding.llError.visibility = View.VISIBLE
            binding.noPayloadText.text = requireActivity()
                .resources.getString(R.string.all_clients_synced)
            binding.noPayloadIcon.setImageResource(R.drawable.ic_assignment_turned_in_black_24dp)
        }
    }

    override fun showProgressbar(b: Boolean) {
        if (b) {
            showMifosProgressBar()
        } else {
            hideMifosProgressBar()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_sync, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_sync) {
            when (userStatus) {
                0 -> if (clientPayloads!!.isNotEmpty()) {
                    mClientSyncIndex = 0
                    syncClientPayload()
                } else {
                    show(
                        binding.root,
                        activity!!.resources.getString(R.string.nothing_to_sync)
                    )
                }

                1 -> showOfflineModeDialog()
                else -> {}
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun syncClientPayload() {
        for (i in clientPayloads!!.indices) {
            if (clientPayloads!![i].errorMessage == null) {
                mSyncPayloadsPresenter.syncClientPayload(clientPayloads!![i])
                mClientSyncIndex = i
                break
            } else {
                Log.d(
                    LOG_TAG,
                    activity!!.resources.getString(R.string.error_fix_before_sync) +
                            clientPayloads!![i].errorMessage
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideMifosProgressBar()
        mSyncPayloadsPresenter.detachView()
    }

    companion object {
        @JvmStatic
        fun newInstance(): SyncClientPayloadsFragment {
            val arguments = Bundle()
            val fragment = SyncClientPayloadsFragment()
            fragment.arguments = arguments
            return fragment
        }
    }
}