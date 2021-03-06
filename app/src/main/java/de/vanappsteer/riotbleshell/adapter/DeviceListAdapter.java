package de.vanappsteer.riotbleshell.adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.polidea.rxandroidble2.RxBleDevice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import de.vanappsteer.riotbleshell.R;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.MyViewHolder> {

    private List<RxBleDevice> mDevices;
    private OnDeviceSelectionListener mOnDeviceSelectionListener;

    static class MyViewHolder extends RecyclerView.ViewHolder {

        private View mRootView;

        MyViewHolder(View v) {
            super(v);
            mRootView = v;
        }

        View getRootView() {
            return mRootView;
        }
    }

    public static abstract class OnDeviceSelectionListener {

        public abstract void onDeviceSelected(RxBleDevice device);
    }

    public DeviceListAdapter() {
        mDevices = new ArrayList<>();
    }

    public DeviceListAdapter(List<RxBleDevice> myDataset) {
        mDevices = myDataset;
    }

    @NonNull
    @Override
    public DeviceListAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_list_item, parent, false);

        return new MyViewHolder(rootView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {

        Button connectButton = holder.getRootView().findViewById(R.id.button_connect);
        connectButton.setOnClickListener(view -> mOnDeviceSelectionListener.onDeviceSelected(mDevices.get(holder.getAdapterPosition())));

        String deviceName = mDevices.get(position).getName();
        if (deviceName == null) {
            deviceName = "";
        }

        TextView textViewDeviceName = holder.getRootView().findViewById(R.id.textview_device_name);
        textViewDeviceName.setText(deviceName);

        TextView textViewDeviceAddress = holder.getRootView().findViewById(R.id.textview_device_address);
        textViewDeviceAddress.setText(mDevices.get(position).getMacAddress());

    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    public void setDevices(Set<RxBleDevice> deviceSet) {

        mDevices.clear();
        mDevices.addAll(deviceSet);

        Collections.sort(mDevices, new DeviceComparator());
    }

    public void setOnDeviceSelectionListener(OnDeviceSelectionListener listener) {
        mOnDeviceSelectionListener = listener;
    }

    private class DeviceComparator implements Comparator<RxBleDevice> {

        public int compare(RxBleDevice first, RxBleDevice second) {

            if (first.getName() == null && second.getName() == null) {
                return first.getMacAddress().compareTo(second.getMacAddress());
            }
            else if (first.getName() == null) {
                return 1;
            }
            else if (second.getName() == null) {
                return -1;
            }

            if (first.getName().compareTo(second.getName()) == 0) {
                return first.getMacAddress().compareTo(second.getMacAddress());
            }
            else {
                return first.getName().compareTo(second.getName());
            }
        }
    }
}
