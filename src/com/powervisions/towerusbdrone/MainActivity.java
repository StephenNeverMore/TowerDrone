package com.powervisions.towerusbdrone;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionResult;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;

public class MainActivity extends Activity implements TowerListener,
		DroneListener {

	private ControlTower controlTower;
	private Drone drone;
	private int droneType = Type.TYPE_UNKNOWN;
	private final Handler handler = new Handler();
	Spinner modeSelector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.controlTower = new ControlTower(this);
		this.drone = new Drone();

		this.modeSelector = (Spinner) findViewById(R.id.modeSelect);
		this.modeSelector
				.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						onFlightModeSelected(view);
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
					}
				});
	}

	protected void onFlightModeSelected(View view) {
		VehicleMode vehicleMode = (VehicleMode) this.modeSelector
				.getSelectedItem();
		this.drone.changeVehicleMode(vehicleMode);
	}

	@Override
	protected void onStart() {
		super.onStart();
		this.controlTower.connect(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (this.drone.isConnected()) {
			this.drone.disconnect();
			updateConnectedButton(false);
		}
		this.controlTower.disconnect();
	}

	protected void updateConnectedButton(boolean isConnected) {
		Button connectButton = (Button) findViewById(R.id.btnConnect);
		if (isConnected) {
			connectButton.setText("Disconnect");
		} else {
			connectButton.setText("Connect");
		}
	}

	public void onBtnConnectTap(View view) {
		if (this.drone.isConnected()) {
			this.drone.disconnect();
		} else {
			Bundle extraParams = new Bundle();
			extraParams.putInt(ConnectionType.EXTRA_USB_BAUD_RATE, 57600);
			ConnectionParameter connetcionParameter = new ConnectionParameter(
					ConnectionType.TYPE_USB, extraParams, null);
			this.drone.connect(connetcionParameter);

		}
	}

	@Override
	public void onDroneConnectionFailed(ConnectionResult arg0) {
		
	}

	@Override
	public void onDroneEvent(String event, Bundle extras) {
		switch (event) {
		case AttributeEvent.STATE_CONNECTED:
			alertUser("Drone Connected");
			updateConnectedButton(this.drone.isConnected());
			updateArmButton();

			break;

		case AttributeEvent.STATE_DISCONNECTED:
			alertUser("Drone Disconnected");
			updateConnectedButton(this.drone.isConnected());
			updateArmButton();
			break;

		case AttributeEvent.STATE_UPDATED:
		case AttributeEvent.STATE_ARMING:
			updateArmButton();
			break;

		case AttributeEvent.TYPE_UPDATED:
			Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
			if (newDroneType.getDroneType() != this.droneType) {
				this.droneType = newDroneType.getDroneType();
				updateVehicleModesForType(this.droneType);
			}
			break;

		case AttributeEvent.STATE_VEHICLE_MODE:
			updateVehicleMode();
			break;

		case AttributeEvent.SPEED_UPDATED:
			updateAltitude();
			updateSpeed();
			break;

		case AttributeEvent.HOME_UPDATED:
			updateDistanceFromHome();
			break;
		default:
			break;
		}
	}

	private void updateDistanceFromHome() {
		TextView distanceTextView = (TextView) findViewById(R.id.distanceValueTextView);
		Altitude droneAltitude = this.drone
				.getAttribute(AttributeType.ALTITUDE);
		double vehicleAltitude = droneAltitude.getAltitude();
		Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
		LatLong vehiclePosition = droneGps.getPosition();

		double distanceFromHome = 0;

		if (droneGps.isValid()) {
			LatLongAlt vehicle3DPosition = new LatLongAlt(
					vehiclePosition.getLatitude(),
					vehiclePosition.getLongitude(), vehicleAltitude);
			Home droneHome = this.drone.getAttribute(AttributeType.HOME);
			distanceFromHome = distanceBetweenPoints(droneHome.getCoordinate(),
					vehicle3DPosition);
		} else {
			distanceFromHome = 0;
		}

		distanceTextView
				.setText(String.format("%3.1f", distanceFromHome) + "m");
	}

	private double distanceBetweenPoints(LatLongAlt pointA, LatLongAlt pointB) {
		if (pointA == null || pointB == null) {
			return 0;
		}
		double dx = pointA.getLatitude() - pointB.getLatitude();
		double dy = pointA.getLongitude() - pointB.getLongitude();
		double dz = pointA.getAltitude() - pointB.getAltitude();
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	private void updateSpeed() {
		TextView speedTextView = (TextView) findViewById(R.id.speedValueTextView);
		Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
		String speedvale = String.format("%3.1f", droneSpeed.getGroundSpeed());
		speedTextView.setText(String.format("%3.1f",
				droneSpeed.getGroundSpeed())
				+ "m/s");
	}

	private void updateAltitude() {
		TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValueTextView);
		Altitude droneAltitude = this.drone
				.getAttribute(AttributeType.ALTITUDE);
		altitudeTextView.setText(String.format("%3.1f",
				droneAltitude.getAltitude())
				+ "m");
	}

	private void updateVehicleModesForType(int droneType2) {
		List<VehicleMode> vehicleModes = VehicleMode
				.getVehicleModePerDroneType(droneType);
		ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(
				this, android.R.layout.simple_spinner_item, vehicleModes);
		vehicleModeArrayAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		this.modeSelector.setAdapter(vehicleModeArrayAdapter);
	}

	private void updateVehicleMode() {
		State vehicleState = this.drone.getAttribute(AttributeType.STATE);
		VehicleMode vehicleMode = vehicleState.getVehicleMode();
		@SuppressWarnings("unchecked")
		ArrayAdapter<VehicleMode> arrayAdapter = (ArrayAdapter<VehicleMode>) this.modeSelector
				.getAdapter();
		this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
	}

	@Override
	public void onDroneServiceInterrupted(String arg0) {

	}

	@Override
	public void onTowerConnected() {
		this.controlTower.registerDrone(this.drone, this.handler);
		this.drone.registerDroneListener(this);
	}

	@Override
	public void onTowerDisconnected() {

	}

	public void alertUser(String msg) {
		Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
	}

	protected void updateArmButton() {
		State vehicleState = this.drone.getAttribute(AttributeType.STATE);
		Button armButton = (Button) findViewById(R.id.btnArmTakeOff);

		if (!this.drone.isConnected()) {
			armButton.setVisibility(View.INVISIBLE);
		} else {
			armButton.setVisibility(View.VISIBLE);
		}

		if (vehicleState.isFlying()) {
			// Land
			armButton.setText("LAND");
		} else if (vehicleState.isArmed()) {
			// Take off
			armButton.setText("TAKE OFF");
		} else if (vehicleState.isConnected()) {
			// Connected but not Armed
			armButton.setText("ARM");
		}
	}

	@SuppressWarnings("unused")
	public void onArmButtonTap(View view) {
		Button thisButton = (Button) view;
		State vehicleState = this.drone.getAttribute(AttributeType.STATE);

		if (vehicleState.isFlying()) {
			// Land
			this.drone.changeVehicleMode(VehicleMode.COPTER_LAND);
		} else if (vehicleState.isArmed()) {
			// Take off
			this.drone.doGuidedTakeoff(10); // Default take off altitude is 10m
		} else if (!vehicleState.isConnected()) {
			// Connect
			alertUser("Connect to a drone first");
		} else if (vehicleState.isConnected() && !vehicleState.isArmed()) {
			// Connected but not Armed
			this.drone.arm(true); 
		}
	}
}
