package de.othaw.labyrinthion;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.MyViewHolder> {

        private Context context;

        private ArrayList id ,name, time;

        CustomAdapter(Context context,ArrayList id ,ArrayList name, ArrayList time){
            this.context = context;
            this.id = id;
            this.name = name;
            this.time = time;
        }




    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view =  inflater.inflate(R.layout.my_row,parent , false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            holder.id_text.setText(String.valueOf(id.get(position)));
            holder.name_text.setText(String.valueOf(name.get(position)));
            holder.time_text.setText(String.valueOf(time.get(position)));
    }

    @Override
    public int getItemCount() {
        return name.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

            TextView id_text,name_text, time_text;


            public MyViewHolder(@NonNull View itemView) {
            super(itemView);
                id_text = itemView.findViewById(R.id.id_text);
                name_text = itemView.findViewById(R.id.name_text);
                time_text = itemView.findViewById(R.id.time_text);


        }
    }
}
