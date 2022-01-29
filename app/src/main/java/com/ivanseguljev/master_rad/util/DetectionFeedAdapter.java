package com.ivanseguljev.master_rad.util;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ivanseguljev.master_rad.R;

import java.util.ArrayList;
import java.util.List;

public class DetectionFeedAdapter extends RecyclerView.Adapter<DetectionFeedAdapter.ViewHolder> {
    List<Bitmap> detections;
    public DetectionFeedAdapter(){
        this.detections=new ArrayList<>();
    }
    @NonNull
    @Override
    public DetectionFeedAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.detectionfeed_row, parent, false);

        return new ViewHolder(itemView);
    }
    public List<Bitmap> getDetections(){
        return detections;
    }
    private void limitListToXElems(){
        if (detections.size()>20){
            detections.remove(20);
        }
    }

    public void addDetection(Bitmap bitmap){

        detections.add(0,bitmap);
        limitListToXElems();
        this.notifyDataSetChanged();
    }


    @Override
    public void onBindViewHolder(@NonNull DetectionFeedAdapter.ViewHolder holder, int position) {
        Bitmap image = this.detections.get(position);
        holder.imageview_detection_feed_image.setImageBitmap(image);
    }

    @Override
    public int getItemCount() {
        return this.detections.size();
    }
    public class ViewHolder extends RecyclerView.ViewHolder{
        public ImageView imageview_detection_feed_image;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageview_detection_feed_image = itemView.findViewById(R.id.imageview_detection_feed_image);
        }
    }
}
