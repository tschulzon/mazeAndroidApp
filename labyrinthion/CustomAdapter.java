package de.othaw.labyrinthion;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;


/**
 * Adapter der fuer den ViewHolder der Datenbank verantwortlich ist
 */
public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.MyViewHolder> {

        private final Context context;

        private final ArrayList id ,name, time;

        CustomAdapter(Context context,ArrayList id ,ArrayList name, ArrayList time){
            this.context = context;
            this.id = id;
            this.name = name;
            this.time = time;
        }

    /**
     * ViewHolder erstellen
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return
     */

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view =  inflater.inflate(R.layout.my_row,parent , false);
        return new MyViewHolder(view);
    }

    /**
     * Daten an den ViewHolder binden
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            holder.id_text.setText(String.valueOf(id.get(position)));
            holder.name_text.setText(String.valueOf(name.get(position)));
            holder.time_text.setText(String.valueOf(time.get(position)));
    }

    /**
     * Anzahl der Elemente in der Liste zurückgeben
     */

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
