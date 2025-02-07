package com.mifos.mifosxdroid.activity.pinpointclient

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.navigation.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.location.places.ui.PlacePicker
import com.mifos.mifosxdroid.R
import com.mifos.mifosxdroid.adapters.PinpointClientAdapter
import com.mifos.mifosxdroid.core.MaterialDialog
import com.mifos.mifosxdroid.core.MifosBaseActivity
import com.mifos.mifosxdroid.core.util.Toaster.show
import com.mifos.mifosxdroid.databinding.ActivityPinpointLocationBinding
import com.mifos.objects.client.ClientAddressRequest
import com.mifos.objects.client.ClientAddressResponse
import com.mifos.utils.CheckSelfPermissionAndRequest.checkSelfPermission
import com.mifos.utils.CheckSelfPermissionAndRequest.requestPermission
import com.mifos.utils.Constants
import javax.inject.Inject

/**
 * @author fomenkoo
 */
class PinpointClientActivity : MifosBaseActivity(), PinPointClientMvpView, OnRefreshListener,
    PinpointClientAdapter.OnItemClick {

    private lateinit var binding: ActivityPinpointLocationBinding
    private val arg : PinpointClientActivityArgs by navArgs()

    @Inject
    lateinit var pinpointClientAdapter: PinpointClientAdapter

    @JvmField
    @Inject
    var pinPointClientPresenter: PinPointClientPresenter? = null
    private var clientId = 0
    private var apptableId : Int? = 0
    private var dataTableId : Int? = 0
    private var addresses: List<ClientAddressResponse> = ArrayList()
    override fun onItemLongClick(position: Int) {
        apptableId = addresses[position].clientId
        dataTableId = addresses[position].id
        MaterialDialog.Builder().init(this)
            .setItems(
                R.array.client_pinpoint_location_options
            ) { dialog, which ->
                when (which) {
                    0 -> if (checkSelfPermission(
                            this@PinpointClientActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    ) {
                        showPlacePiker(REQUEST_UPDATE_PLACE_PICKER)
                    } else {
                        requestPermission(REQUEST_UPDATE_PLACE_PICKER)
                    }

                    1 -> apptableId?.let {
                        dataTableId?.let { it1 ->
                            pinPointClientPresenter?.deleteClientPinpointLocation(
                                it, it1
                            )
                        }
                    }

                    else -> {}
                }
            }
            .createMaterialDialog()
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityComponent?.inject(this)
        binding = ActivityPinpointLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pinPointClientPresenter?.attachView(this)
        showBackButton()
        clientId = arg.clientId
        showUserInterface()
        pinPointClientPresenter?.getClientPinpointLocations(clientId)
    }

    override fun showUserInterface() {
        val mLayoutManager = LinearLayoutManager(this)
        mLayoutManager.orientation = LinearLayoutManager.VERTICAL
        pinpointClientAdapter.setContext(this)
        binding.rvPinpointLocation.layoutManager = mLayoutManager
        binding.rvPinpointLocation.setHasFixedSize(true)
        pinpointClientAdapter.setItemClick(this)
        binding.rvPinpointLocation.adapter = pinpointClientAdapter
        binding.swipeContainer.setColorSchemeColors(
            *this
                .resources.getIntArray(R.array.swipeRefreshColors)
        )
        binding.swipeContainer.setOnRefreshListener(this)
    }

    override fun showPlacePiker(requestCode: Int) {
        try {
            val intentBuilder = PlacePicker.IntentBuilder()
            val intent = intentBuilder.build(this)
            startActivityForResult(intent, requestCode)
        } catch (e: GooglePlayServicesRepairableException) {
            GooglePlayServicesUtil.getErrorDialog(e.connectionStatusCode, this, 0)
        } catch (e: GooglePlayServicesNotAvailableException) {
            Toast.makeText(
                this, getString(R.string.google_play_services_not_available),
                Toast.LENGTH_LONG
            )
                .show()
        }
    }

    /**
     * This Method is Requesting the Permission
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun requestPermission(requestCode: Int) {
        requestPermission(
            this@PinpointClientActivity,
            Manifest.permission.ACCESS_FINE_LOCATION,
            requestCode,
            resources.getString(
                R.string.dialog_message_access_fine_location_permission_denied
            ),
            resources.getString(
                R.string.dialog_message_permission_never_ask_again_fine_location
            ),
            Constants.ACCESS_FINE_LOCATION_STATUS
        )
    }

    /**
     * This Method getting the Response after User Grant or denied the Permission
     *
     * @param requestCode  Request Code
     * @param permissions  Permission
     * @param grantResults GrantResults
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_ADD_PLACE_PICKER -> {
                run {
                    // If request is cancelled, the result arrays are empty.
                    if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    ) {

                        // permission was granted, yay! Do the
                        showPlacePiker(REQUEST_ADD_PLACE_PICKER)
                    } else {

                        // permission denied, boo! Disable the
                        show(
                            findViewById(android.R.id.content),
                            getString(R.string.permission_denied_to_access_fine_location)
                        )
                    }
                }
                run {
                    // If request is cancelled, the result arrays are empty.
                    if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    ) {

                        // permission was granted, yay! Do the
                        showPlacePiker(REQUEST_UPDATE_PLACE_PICKER)
                    } else {

                        // permission denied, boo! Disable the
                        show(
                            findViewById(android.R.id.content),
                            getString(R.string.permission_denied_to_access_fine_location)
                        )
                    }
                }
            }

            REQUEST_UPDATE_PLACE_PICKER -> {
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    showPlacePiker(REQUEST_UPDATE_PLACE_PICKER)
                } else {
                    show(
                        findViewById(android.R.id.content),
                        getString(R.string.permission_denied_to_access_fine_location)
                    )
                }
            }
        }
    }

    override fun onRefresh() {
        binding.llError.visibility = View.GONE
        pinPointClientPresenter?.getClientPinpointLocations(clientId)
    }

    override fun showClientPinpointLocations(clientAddressResponses: List<ClientAddressResponse>) {
        binding.llError.visibility = View.GONE
        addresses = clientAddressResponses
        pinpointClientAdapter.setAddress(clientAddressResponses)
    }

    override fun showFailedToFetchAddress() {
        binding.llError.visibility = View.VISIBLE
        binding.tvNoLocation.text = getString(R.string.failed_to_fetch_pinpoint_location)
    }

    override fun showEmptyAddress() {
        binding.llError.visibility = View.VISIBLE
        binding.tvNoLocation.text = getString(R.string.empty_client_address)
    }

    override fun updateClientAddress(message: Int) {
        showMessage(message)
        pinPointClientPresenter?.getClientPinpointLocations(clientId)
    }

    override fun showProgressbar(show: Boolean) {
        binding.swipeContainer.isRefreshing = show
    }

    override fun showProgressDialog(show: Boolean, message: Int?) {
        if (show) {
            showProgress(getString(message!!))
        } else {
            hideProgress()
        }
    }

    override fun showMessage(message: Int) {
        show(findViewById(android.R.id.content), getString(message))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_client_save_pin, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.save_pin) {
            if (checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                showPlacePiker(REQUEST_ADD_PLACE_PICKER)
            } else {
                requestPermission(REQUEST_ADD_PLACE_PICKER)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            val place = PlacePicker.getPlace(this@PinpointClientActivity, data)
            val clientAddressRequest = ClientAddressRequest(
                place.id,
                place.address.toString(),
                place.latLng.latitude,
                place.latLng.longitude
            )
            if (requestCode == REQUEST_ADD_PLACE_PICKER) {
                pinPointClientPresenter?.addClientPinpointLocation(clientId, clientAddressRequest)
            } else if (requestCode == REQUEST_UPDATE_PLACE_PICKER) {
                apptableId?.let {
                    dataTableId?.let { it1 ->
                        pinPointClientPresenter?.updateClientPinpointLocation(
                            it, it1,
                            clientAddressRequest
                        )
                    }
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pinPointClientPresenter?.detachView()
    }

    companion object {
        private const val REQUEST_ADD_PLACE_PICKER = 1
        private const val REQUEST_UPDATE_PLACE_PICKER = 2
    }
}