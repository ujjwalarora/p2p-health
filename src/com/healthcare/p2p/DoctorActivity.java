package com.healthcare.p2p;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.healthcare.beans.EncryptedMessageBean;
import com.healthcare.beans.MessageBean;
import com.healthcare.beans.SectionDataBean;
import com.healthcare.taghelper.Tools;

public class DoctorActivity extends FragmentActivity implements ActionBar.TabListener {

	private static final String PRESCRIPTION_MSG_DOMAIN = "com.healthcare.beam";
	private static final String PRESCRIPTION_MSG_TYPE = "prescriptionv1";
	private static String KEY = "secretkey";
	
	private static TextView detailsView;
	private static EditText doctorNameView;
	private static EditText problemView;
	private static EditText testView;
	private static EditText medicineView;
	
	private static String details;
	private static String doctorName;
	private static String problem;
	private static String test;
	private static String medicine;
	
	private NfcAdapter mNfcAdapter;
	private Activity activity;
	private Context c;
	
	private SectionsPagerAdapter mSectionsPagerAdapter;
	private ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_doctor);
		
		activity = this;
		c = getApplicationContext();

		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
			}
		});

		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			actionBar.addTab(actionBar.newTab().setText(mSectionsPagerAdapter.getPageTitle(i)).setTabListener(this));
		}
		
		initNFC();
	}
	
	void initNFC() {
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		
		// Register callback to set NDEF message
        mNfcAdapter.setNdefPushMessageCallback(new CreateNdefMessageCallback() {
			@Override
			public NdefMessage createNdefMessage(NfcEvent event) {
				byte toSend[] = null;
				getData();
				// Check if second page is selected
				if(mViewPager.getCurrentItem() == 1) {
					try {
						if(validateData())
							toSend = Tools.objectToByteArray(new SectionDataBean(doctorName, problem, test, medicine, Integer.parseInt(LoginActivity.section)));
						else
							showToast("Please enter details");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
				NdefMessage msg = new NdefMessage(NdefRecord.createExternal(PRESCRIPTION_MSG_DOMAIN, PRESCRIPTION_MSG_TYPE, toSend));
				return msg;
			}
		}, this);
        
        // Register callback to listen for message-sent success
        mNfcAdapter.setOnNdefPushCompleteCallback(new OnNdefPushCompleteCallback() {
			@Override
			public void onNdefPushComplete(NfcEvent event) {
				showToast("Prescription sent");
				resetViews();
			}
		}, this);
	}
	
	void showToast(final String msg) {
		activity.runOnUiThread(new Runnable() {
		    public void run() {
		        Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
		    }
		});
	}
	
	void resetViews() {
		activity.runOnUiThread(new Runnable() {
		    public void run() {
		    	if(detailsView != null) detailsView.setText("");
				if(doctorNameView != null) doctorNameView.setText("");
				if(problemView != null) problemView.setText("");
				if(testView != null) testView.setText("");
				if(medicineView != null) medicineView.setText("");
		    }
		});
	}
	
	void getData() {
		if(doctorNameView != null) doctorName = doctorNameView.getText().toString();
		if(problemView != null) problem = problemView.getText().toString();
		if(testView != null) test = testView.getText().toString();
		if(medicineView != null) medicine = medicineView.getText().toString();
	}
	
	static void setData() {
		if(detailsView != null) detailsView.setText(details);
		if(doctorNameView != null) doctorNameView.setText(doctorName);
		if(problemView != null) problemView.setText(problem);
		if(testView != null) testView.setText(test);
		if(medicineView != null) medicineView.setText(medicine);
	}
	
	boolean validateData() {
		if(doctorName.equals("") || problem.equals("") || test.equals("") || medicine.equals(""))
			return false;
		return true;
	}

	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			Fragment fragment = null;
			switch(position) {
			case 0: fragment = new DetailsFragment();
				break;
			case 1: fragment = new PrescriptionFragment();
				break;
			}
			return fragment;
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0:
				return getString(R.string.title_section1).toUpperCase();
			case 1:
				return getString(R.string.title_section2).toUpperCase();
			}
			return null;
		}
	}

	public static class DetailsFragment extends Fragment {
		public DetailsFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_doctor_details, container, false);
			detailsView = (TextView) rootView.findViewById(R.id.doctor_details_info);
			setData();
			return rootView;
		}
	}
	
	public static class PrescriptionFragment extends Fragment {
		public PrescriptionFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_doctor_prescription, container, false);
			doctorNameView = (EditText) rootView.findViewById(R.id.prescription_doctor_name);
			problemView = (EditText) rootView.findViewById(R.id.prescription_problem);
			testView = (EditText) rootView.findViewById(R.id.prescription_test);
			medicineView = (EditText) rootView.findViewById(R.id.prescription_medicine);
			setData();
			return rootView;
		}
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
		resetViews();
		details = "";
		Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		NdefMessage msg = (NdefMessage) rawMsgs[0];
		// record 0 contains the MIME type, record 1 is the AAR, if present
		EncryptedMessageBean emb = null;
		MessageBean mb = null;
		try {
			Object receivedMsg = Tools.byteArrayToObject(msg.getRecords()[0].getPayload());
			emb = (EncryptedMessageBean) receivedMsg;
			KEY = LoginActivity.priv_key;
			mb = Tools.getDencryptedMessageBean(emb, KEY);
			
			// Debug
			System.out.println("Message received by doctor:\n" + mb);
			
			if(mb != null)
				details = mb.toString();
			
		} catch (Exception e) {
			e.printStackTrace();
			showToast("Received message wasn't recognized");
		}
		showToast("Health record received");
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

	@Override
	public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
		
	}

	@Override
	public void onTabSelected(Tab arg0, FragmentTransaction arg1) {
		
	}

	@Override
	public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
		
	}
}
