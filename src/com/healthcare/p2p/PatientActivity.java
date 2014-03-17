package com.healthcare.p2p;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.healthcare.beans.SectionDataBean;
import com.healthcare.taghelper.Tools;

public class PatientActivity extends Activity {

	private static final String HEALTH_RECORD_MSG_DOMAIN = "com.healthcare.beam";
	private static final String HEALTH_RECORD_MSG_TYPE = "ehealthrecordv1";
	private static final String healthRecordPath = Environment.getExternalStorageDirectory() + "/healthrecord.txt";
	private static String KEY = "secretkey";
	
	private NfcAdapter mNfcAdapter;
	private TextView mInfoText;
	private RadioGroup mRadioGroup;
	private Activity activity;
	private Context c;
	int selected = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_patient);
		
		activity = this;
		c = getApplicationContext();
		
		mInfoText = (TextView) findViewById(R.id.patient_info);
		mRadioGroup = (RadioGroup) findViewById(R.id.radiogroup);
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		
		// Add RadioButtons
		for(int i=0; i<LoginActivity.name.size(); i++) {
			String name = LoginActivity.name.get(i);
			String email = LoginActivity.email.get(i);
			RadioButton rb = new RadioButton(c);
			rb.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			rb.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
			rb.setTextAppearance(getApplicationContext(), android.R.style.TextAppearance_Holo_Medium_Inverse);
			rb.setText(name + " (" + email + ")");
			rb.setId(i);
			if(i == 0)
				rb.setChecked(true);
			mRadioGroup.addView(rb);
		}
		
		mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup arg0, int arg1) {
				RadioButton checkedRadioButton = (RadioButton)arg0.findViewById(arg1);
				selected = checkedRadioButton.getId();
			}
		});
		
		if (mNfcAdapter == null) {
            Toast.makeText(getApplicationContext(), "NFC not available", Toast.LENGTH_SHORT).show();
        } else {
        	
            // Register callback to set NDEF message
            mNfcAdapter.setNdefPushMessageCallback(new CreateNdefMessageCallback() {
				@Override
				public NdefMessage createNdefMessage(NfcEvent event) {
					byte toSend[] = null;
					try {
						KEY = LoginActivity.pub_key.get(selected);
						
						//Debug
						System.out.println("Selected : " + selected);
						System.out.println("Key : " + KEY);
						
						toSend = Tools.objectToByteArray(Tools.getEncryptedMessageBean(healthRecordPath, KEY));
					} catch (Exception e) {
						e.printStackTrace();
					}
					NdefMessage msg = new NdefMessage(NdefRecord.createExternal(HEALTH_RECORD_MSG_DOMAIN, HEALTH_RECORD_MSG_TYPE, toSend));
					return msg;
				}
			}, this);
            
            // Register callback to listen for message-sent success
            mNfcAdapter.setOnNdefPushCompleteCallback(new OnNdefPushCompleteCallback() {
				@Override
				public void onNdefPushComplete(NfcEvent event) {
					activity.runOnUiThread(new Runnable() {
					    public void run() {
					        showToast("Health record sent");
					    }
					});
				}
			}, this);
        }
		
		setViews();
	}
	
	void setViews() {
		try {
			mInfoText.setText(Tools.readHealthRecordFromFile(healthRecordPath).toString());
		} catch (Exception e) {
			e.printStackTrace();
			showToast("No heath record found");
		}
	}
	
	void showToast(final String msg) {
		activity.runOnUiThread(new Runnable() {
		    public void run() {
		        Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
		    }
		});
	}
	
	@Override
	public void onResume() {
	super.onResume();
	if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction()))
		processIntent(getIntent());
	}
	
	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent);
	}
	
	void processIntent(Intent intent) {
		Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		NdefMessage msg = (NdefMessage) rawMsgs[0];
		// record 0 contains the MIME type, record 1 is the AAR, if present
		SectionDataBean sdb = null;
		try {
			Object receivedMsg = Tools.byteArrayToObject(msg.getRecords()[0].getPayload());
			sdb = (SectionDataBean) receivedMsg;
			
			if(LoginActivity.name.get(selected).equals(sdb.getDoctor())) {
				// Debug
				System.out.println("Message received by patient:\n" + sdb);
				Tools.addDataToSectioniOfHealthRecordFile(healthRecordPath, sdb, sdb.getSectionNum());
				showToast("Prescription added to health record");
			}
			else {
				showToast("Unauthorized doctor");
			}
		} catch (Exception e) {
			e.printStackTrace();
			showToast("Received message wasn't recognized");
		}
		
		setViews();
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // If NFC is not available, we won't be needing this menu
        if (mNfcAdapter == null) {
            return super.onCreateOptionsMenu(menu);
        }
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent intent = new Intent(Settings.ACTION_NFCSHARING_SETTINGS);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
