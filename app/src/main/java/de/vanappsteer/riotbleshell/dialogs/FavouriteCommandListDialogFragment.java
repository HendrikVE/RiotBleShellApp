package de.vanappsteer.riotbleshell.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import de.vanappsteer.riotbleshell.R;
import de.vanappsteer.riotbleshell.util.GsonObjectStorage;
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class FavouriteCommandListDialogFragment extends DialogFragment {

    private Context mContext;

    private View mDialogView;
    private GsonObjectStorage mGsonObjectStorage;
    private OnCommandSelectedListener mOnCommandSelectedListener;

    public static FavouriteCommandListDialogFragment newInstance() {

        return new FavouriteCommandListDialogFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mContext = context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        mDialogView = View.inflate(mContext, R.layout.dialog_favourite_commands, null);

        mGsonObjectStorage = GsonObjectStorage.getInstance(mContext);

        Type type = new TypeToken<ArrayList<String>>(){}.getType();
        ArrayList<String> commandList = mGsonObjectStorage.loadObject(R.string.sp_string_gson_favourite_commands, type);

        if (commandList == null) {
            commandList = new ArrayList<>();
        }

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mContext,
                DividerItemDecoration.VERTICAL);
        MyAdapter myAdapter = new MyAdapter(commandList);

        RecyclerView recyclerView = mDialogView.findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setAdapter(myAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                new SwipeToDeleteCallback(myAdapter, 0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        super.onSwiped(viewHolder, direction);

                        storeFavouriteCommandList(myAdapter.getDataset());
                    }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);

        Button addButton = mDialogView.findViewById(R.id.button_add);
        TextInputEditText textInputEditText = mDialogView.findViewById(R.id.textinputedittext_new_command);

        addButton.setOnClickListener(view -> {

            Editable text = textInputEditText.getText();
            String newCommand = "";

            if (text != null) {
                newCommand = text.toString();
            }

            if (newCommand.length() > 0) {
                myAdapter.addItem(newCommand);
                storeFavouriteCommandList(myAdapter.getDataset());
            }

            textInputEditText.setText("");

            recyclerView.smoothScrollToPosition(myAdapter.getItemCount());

            InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            textInputEditText.clearFocus();
        });

        Button okButton = mDialogView.findViewById(R.id.button_ok);
        okButton.setOnClickListener(view -> {
            dismiss();
        });


        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setTitle("Choose a command")
                .setView(mDialogView);

        return builder.create();
    }

    public void setOnCommandSelectedListener(OnCommandSelectedListener listener) {
        mOnCommandSelectedListener = listener;
    }

    private void storeFavouriteCommandList(ArrayList<String> commandList) {
        mGsonObjectStorage.storeObject(R.string.sp_string_gson_favourite_commands, commandList);
    }

    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

        private ArrayList<String> mDataset;

        public class MyViewHolder extends RecyclerView.ViewHolder {

            private View rootView;

            private MyViewHolder(View v) {
                super(v);
                rootView = v;
            }
        }

        private MyAdapter(ArrayList<String> myDataset) {
            mDataset = myDataset;
        }

        @NonNull
        @Override
        public MyAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.listitem_favourite_command, parent, false);

            return new MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {

            String value = mDataset.get(position);

            TextView textView = holder.rootView.findViewById(R.id.textview);
            textView.setText(value);

            holder.rootView.setOnClickListener(view -> {
                if (mOnCommandSelectedListener != null) {
                    mOnCommandSelectedListener.onCommandSelected(value);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mDataset.size();
        }

        public void removeItem(int position) {
            mDataset.remove(position);
            notifyDataSetChanged();
        }

        public void addItem(String item) {
            mDataset.add(item);
            notifyDataSetChanged();
        }

        public ArrayList<String> getDataset() {
            return mDataset;
        }
    }

    private abstract class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

        private MyAdapter mMyAdapter;

        private SwipeToDeleteCallback(MyAdapter myAdapter, int dragDirs, int swipeDirs) {
            super(dragDirs, swipeDirs);

            mMyAdapter = myAdapter;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        public void onChildDraw (@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                 @NonNull RecyclerView.ViewHolder viewHolder,
                                 float dX, float dY, int actionState, boolean isCurrentlyActive){

            new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    .addBackgroundColor(Color.RED)
                    .addActionIcon(R.drawable.ic_delete_white)
                    .create()
                    .decorate();

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            mMyAdapter.removeItem(viewHolder.getAdapterPosition());
        }
    }

    public static abstract class OnCommandSelectedListener {
        public abstract void onCommandSelected(String command);
    }
}
