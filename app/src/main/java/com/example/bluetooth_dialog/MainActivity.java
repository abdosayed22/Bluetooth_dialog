package com.example.bluetooth_dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private CompanionDeviceManager deviceManager;
    private AssociationRequest pairingRequest;
    private BluetoothDeviceFilter deviceFilter;

    private static final int SELECT_DEVICE_REQUEST_CODE = 42;
    private TextView status;
    private Button btnConnect;
    private ListView listView;
    private Dialog dialog;
    private EditText inputLayout;
    private ArrayAdapter<String> chatAdapter;
    private ArrayList<String> chatMessages;
    private BluetoothAdapter bluetoothAdapter;
    private ImageView image;
    InputStream inputStream=null;
    private byte[]sora;
    boolean ch = false ;

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_OBJECT = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int image_read = 6;
   // public static final int MESSAGE_STATE_CHANGE_image=7;
    public static final String DEVICE_OBJECT = "device_name";
   // public  static boolean sendimage=true;

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private ChatController chatController;
    private sendimage Sendimage;
    private BluetoothDevice connectingDevice;
    private ArrayAdapter<String> discoveredDevicesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewsByIds();

        //check device support bluetooth or not
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available!", Toast.LENGTH_SHORT).show();
            finish();
        }

        //show bluetooth devices dialog when click connect button
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPrinterPickDialog();


                    }
                });



        //set chat adapter
        chatMessages = new ArrayList<>();
        chatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, chatMessages);
        listView.setAdapter(chatAdapter);
    }

    private Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case ChatController.STATE_CONNECTED:
                           setStatus("Connected to: " + connectingDevice.getName());
                            break;
                        case ChatController.STATE_CONNECTING:
                            setStatus("Connecting...");
                         //   btnConnect.setEnabled(false);
                            break;
                        case ChatController.STATE_LISTEN:
                        case ChatController.STATE_NONE:
                            setStatus("Not connected");
                            break;
                    }
                    break;
               /* case MESSAGE_STATE_CHANGE_image:
                    switch (msg.arg1) {
                        case sendimage.STATE_CONNECTED:
                            setStatus("Connected to: " + connectingDevice.getName());
                            btnConnect.setEnabled(false);
                            break;
                        case sendimage.STATE_CONNECTING:
                            setStatus("Connecting...");
                            btnConnect.setEnabled(false);
                            break;
                        case sendimage.STATE_LISTEN:
                        case sendimage.STATE_NONE:
                            setStatus("Not connected");
                            break;
                    }
                    break;
*/

                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;

                   String writeMessage = new String(writeBuf);
               //    Toast.makeText(getApplicationContext(),writeMessage,Toast.LENGTH_SHORT).show();


                   chatMessages.add("Me: " + writeMessage);
                   chatAdapter.notifyDataSetChanged();
                    break;
                case MESSAGE_READ:

                        byte[] readBuf = (byte[]) msg.obj;
                        String readMessage = new String(readBuf, 0, msg.arg1);
                        chatMessages.add(connectingDevice.getName() + ":  " + readMessage);
                        chatAdapter.notifyDataSetChanged();
                        break;
                case image_read:
                    Toast.makeText(getApplicationContext(),"image",Toast.LENGTH_SHORT).show();
                    byte[]read=(byte[])msg.obj;
                    Bitmap bitmap=BitmapFactory.decodeByteArray(read,0,msg.arg1);
                    image.setImageBitmap(bitmap);
                    //chatMessages.add(connectingDevice.getName() + ":  " + bitmap);
                  //  chatAdapter.notifyDataSetChanged();
                    break;
                case MESSAGE_DEVICE_OBJECT:
                    connectingDevice = msg.getData().getParcelable(DEVICE_OBJECT);
                    Toast.makeText(getApplicationContext(), "Connected to " + connectingDevice.getName(),
                            Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString("toast"),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    private void showPrinterPickDialog() {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.layout_bluetooth);
        dialog.setTitle("Bluetooth Devices");

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();

        //Initializing bluetooth adapters
        ArrayAdapter<String> pairedDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        discoveredDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        //locate listviews and attatch the adapters
        ListView listView = (ListView) dialog.findViewById(R.id.pairedDeviceList);
        ListView listView2 = (ListView) dialog.findViewById(R.id.discoveredDeviceList);
        listView.setAdapter(pairedDevicesAdapter);
        listView2.setAdapter(discoveredDevicesAdapter);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryFinishReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryFinishReceiver, filter);


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();


        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            pairedDevicesAdapter.add(getString(R.string.none_paired));
        }

        //Handling listview item click event
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothAdapter.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                 ch = true ;
                connectToDevice(address);
                dialog.dismiss();
            }

        });

        listView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bluetoothAdapter.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                connectToDevice(address);
                dialog.dismiss();
            }
        });

        dialog.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    private void setStatus(String s) {
        status.setText(s);
    }

    private void connectToDevice(String deviceAddress) {
        bluetoothAdapter.cancelDiscovery();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        chatController.connect(device);
       // Sendimage.connect(device);
    }

    private void findViewsByIds() {
        status = (TextView) findViewById(R.id.status);
        btnConnect = (Button) findViewById(R.id.btn_connect);
        listView = (ListView) findViewById(R.id.list);
        inputLayout = (EditText) findViewById(R.id.input_layout);
        View btnSend = findViewById(R.id.btn_send);
       image = (ImageView) findViewById(R.id.button);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                opengallery();
            }
        });



        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (inputLayout.getText().toString().equals("")) {
                    Toast.makeText(MainActivity.this, "Please input some texts", Toast.LENGTH_SHORT).show();
                } else {
                    //TODO: here
                    // sendMessage();
                    sendMessage(inputLayout.getText().toString());
                    inputLayout.setText("");
                    //}
                }
            }
        });
    }

    public void opengallery(){
        Toast.makeText(getApplicationContext(),"swr",Toast.LENGTH_SHORT).show();
        Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent,100);
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


            if (resultCode == RESULT_OK) {

                if (requestCode == 100) {
                    //  sendimage=true;
                    //   Toast.makeText(getApplicationContext() ,"trueeeeeeeeeeeeeeeeeeee" , Toast.LENGTH_SHORT ).show();


                    Toast.makeText(getApplicationContext(), "gbtha", Toast.LENGTH_SHORT).show();

                    Uri uri = data.getData();
                    // chatController = new ChatController(this,handler);

                    try {

                        // chatController = new ChatController(this,handler);
                        inputStream = getContentResolver().openInputStream(uri);
                        Bitmap decodestream = BitmapFactory.decodeStream(inputStream);
                        sora = getBitmapAsByteArray(decodestream);
                        int Arraysize = 400;
                        chatController.write(String.valueOf(sora.length).getBytes());
                        //  String msg = "blah";
                        //byte[] msgbyte = new byte[msg.length()];
                        //msgbyte = msg.getBytes();


                        for (int i = 0; i < sora.length; i += Arraysize) {
                            byte[] temparray;

                            temparray = Arrays.copyOfRange(sora, i, Math.min(sora.length, i + Arraysize));
                            //  byte[] c = new byte[msgbyte.length + temparray.length];

                            //System.arraycopy(temparray, 0, c, 0, temparray.length);
                            //System.arraycopy(msgbyte, 0, c, temparray.length, msgbyte.length);

                            // sendimage(temparray);
                            chatController.write(temparray);


                        }
                        image.setImageBitmap(decodestream);
                        //  chatController.write(sora);


                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                }
            } else {
                Toast.makeText(getApplicationContext(), "no", Toast.LENGTH_SHORT).show();
            }


        switch (requestCode) {
           case REQUEST_ENABLE_BLUETOOTH:
                if (resultCode == Activity.RESULT_OK) {
                    chatController = new ChatController(this, handler);
                } else {
                    Toast.makeText(this, "Bluetooth still disabled, turn off application!", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }

    }

    private void sendMessage(String message) {
        if (chatController.getState() != ChatController.STATE_CONNECTED) {

            Toast.makeText(this, "Connection was lost!", Toast.LENGTH_SHORT).show();
            return;
        }
     /*   if (Sendimage.getState() !=Sendimage.STATE_CONNECTED) {

            Toast.makeText(this, "Connection was lost!", Toast.LENGTH_SHORT).show();
            return;
        }*/

        if(message.length()>0){
            // byte[]send=message.getBytes();
           // chatController.write(send);
           // Sendimage.write(send);
        }




    }


    @Override
    public void onStart() {
        super.onStart();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
          //  Intent discoverable=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
           // discoverable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,10);
            // startActivity(discoverable);
        } else {
            chatController = new ChatController(this, handler);
          //  Sendimage=new sendimage(this,handler);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (chatController != null) {
            if (chatController.getState() == ChatController.STATE_NONE) {
                chatController.start();

            }
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chatController != null)
            chatController.stop();
    }

    private final BroadcastReceiver discoveryFinishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    discoveredDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (discoveredDevicesAdapter.getCount() == 0) {
                    discoveredDevicesAdapter.add(getString(R.string.none_found));
                }
            }
        }
    };
    public static byte[]getBitmapAsByteArray(Bitmap bitmap){
        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        return outputStream.toByteArray();


    }

    
}
