package com.haerokim.project_footprint

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import kotlinx.android.synthetic.main.activity_main.*
import org.altbeacon.beacon.*


class MainActivity : AppCompatActivity(), BeaconConsumer, PermissionListener {
    private lateinit var beaconManager: BeaconManager
    private var beaconList: MutableList<Beacon> = mutableListOf()

    override fun onPermissionGranted() {
        Toast.makeText(this, "권환 획득 완료!", Toast.LENGTH_LONG).show()
    }

    override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
        Toast.makeText(this, "권환 획득 실패", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        TedPermission.with(this)
            .setPermissionListener(this)
            .setDeniedMessage("위치 기반 서비스이므로 위치 정보 권한이 필요합니다.\n\n[설정] > [앱]을 통해 권한 허가를 해주세요.")
            .setPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
            .check();

        beaconManager = BeaconManager.getInstanceForApplication(this)
        beaconManager.getBeaconParsers()
            .add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"))
        beaconManager.bind(this)

        var handler: Handler = @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(msg: Message?) {
                Textview.setText("")

                // 비콘의 아이디와 거리를 측정하여 보여줌
                for (beacon in beaconList) {
                    Textview.append(
                        "ID : " + beacon.id1 + " \n " + "Distance : " + String.format(
                            "%.3f",
                            beacon.distance
                        ).toDouble() + "m\n\n"
                    )

                }

                // 자기 자신을 0.5초마다 호출
                this.sendEmptyMessageDelayed(0, 500)
            }
        }

        val foregroundIntent = Intent(this, ForegroundService::class.java)
        toggle_test.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                //위치 권한 허용 되어있으면 비콘 스캔 시작
                if (TedPermission.isGranted(this)) {
                    val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    if (mBluetoothAdapter == null) {
                        // Device does not support Bluetooth
                    } else if (!mBluetoothAdapter.isEnabled) {
                        // Bluetooth is not enabled :)
                    } else {
                        // Bluetooth is enabled
                        handler.sendEmptyMessage(0)
                        Toast.makeText(this, "비콘 스캔시작", Toast.LENGTH_LONG).show()
                    }
                } else { //워치 권한 X
                    Toast.makeText(this, "앱 사용을 위해 위치 권한이 있어야합니다.", Toast.LENGTH_LONG).show()
                }

                //Foreground Service 시작 (비콘 스캔 서비스)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(foregroundIntent)
                } else {
                    startService(foregroundIntent)
                }

                GetPlaceInfo(applicationContext, "연남동 감칠").execute()

            } else {
                stopService(foregroundIntent)
            }
        }
    }

    override fun onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(RangeNotifier { beacons, region ->
            // 비콘이 감지되면 해당 함수가 호출된다. Collection<Beacon> beacons에는 감지된 비콘의 리스트가,
            // region에는 비콘들에 대응하는 Region 객체가 들어온다.
            if (beacons.size > 0) {
                beaconList.clear()
                for (beacon in beacons) {
                    beaconList.add(beacon)
                }
            }
        })

        try {
            beaconManager.startRangingBeaconsInRegion(Region("myRangingUniqueId", null, null, null))
        } catch (e: RemoteException) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BluetoothAdapter.getDefaultAdapter().disable();
        beaconManager.unbind(this)
    }
}