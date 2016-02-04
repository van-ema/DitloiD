package giava.menmath.ditloid;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.Set;

/**
 * @Author Emanuele Vannacci , Tiziano Menichelli , Simone Mattogno , Gianluca Giallatini
 *
 * @see Challenge
 *
 * The class provides some services for bluetooth connection.
 * MainActivity calls it when the user click on the buttton btnChallenge.
 * On creation state , the class provides a check out of  bluetooth connection service
 * on the device.
 */
public class FriendConnection extends AppCompatActivity {

    private Button btnConnect;

    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothAdapter mBluetoothAdapter;

    private ListView myListView;

    /**
     * contains string with device names and Mac addresses
     */
    private ArrayAdapter<String> BTArrayAdapter;

    /**
     * Bluetooth devices that are already pair.
     */
    private Set<BluetoothDevice> pairedDevices;

    /**
     * Broadcast receveir that provides services to
     */
    private MyBroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_connection);

        btnConnect = (Button) findViewById(R.id.btnConnect);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

//      Check if Bluetooth is supported by device
        if (mBluetoothAdapter == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Bluetooth no supported")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                            dialog.dismiss();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();

        }


//      it makes the local device discoverable to other devices and active bluetooth if it isn't actived
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        mReceiver = new MyBroadcastReceiver();

//      Set Listener. When Click on Button searches new devices to pair
        btnConnect.setOnClickListener(new MyClick());

        myListView.setOnItemClickListener(new MyOnItemClickListener());


//        start manage connection like server
        BServer server = new BServer(mBluetoothAdapter);
        server.start();

    }

    /**
     * Listener for btnConnect. On click it Shows dialog with available devices and allows
     * to select one of them to start game.
     */
    public class MyClick implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

            showBTDialog();
        }
    }


    /**
     * Class provides a Listener for a ListeView. The system shows a dialog with a inner ListView
     * that contains available devices (name and address) for bluetooth connection. The user can
     * choose one of them and the Listener retrieves the correct device through the MAC address.
     * Then Client thread is started.
     */
    public class MyOnItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            String s = BTArrayAdapter.getItem(position);
            String[] parts = s.split("\\s*\\r?\\n\\s*");
            String address = parts[1];

            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

            BClient client = new BClient(device, mBluetoothAdapter);
            client.start();

        }

    }

    /**
     * Class provides method to receive information from devices nearby.
     * The process is asynchronous and it usually involves an inquiry scan of about 12 seconds
     */
    public class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }

    /**
     * Show Dialog allowing user to see available devices for bluetooth connection.
     * If no devices are available a alert dialog is shown.
     */
    public void showBTDialog() {

        final AlertDialog.Builder popDialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View viewLayout = inflater.inflate(R.layout.bt_dialog, (ViewGroup) findViewById(R.id.bt_list));

        popDialog.setTitle("Paired Bluetooth Devices");
        popDialog.setView(viewLayout);

        // create the arrayAdapter that contains the BTDevices, and set it to a ListView
        myListView = (ListView) viewLayout.findViewById(R.id.BTList);

        BTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        myListView.setAdapter(BTArrayAdapter);


        // get paired devices
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // put it's one to the adapter
            for (BluetoothDevice device : pairedDevices)
                BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            // Button Cancel
            popDialog.setPositiveButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }

                    });

            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter);
            mBluetoothAdapter.startDiscovery();

            popDialog.create();
            popDialog.show();

            BTArrayAdapter.clear();

        } else {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("No device connected")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();

        }

    }


    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mBluetoothAdapter != null)
                mBluetoothAdapter.cancelDiscovery();

            unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }


}
