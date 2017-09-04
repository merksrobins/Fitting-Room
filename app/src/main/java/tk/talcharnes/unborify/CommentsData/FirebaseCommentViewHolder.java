package tk.talcharnes.unborify.CommentsData;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import tk.talcharnes.unborify.R;
import tk.talcharnes.unborify.Utilities.FirebaseConstants;

/**
 * Created by Tal on 9/3/2017.
 *
 * THANKS TO: https://www.learnhowtoprogram.com/android/data-persistence/firebase-recycleradapter
 */

public class FirebaseCommentViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private View mView;
    private Context mContext;
    private String mphotoUserID;
    private String mUrl;


    public FirebaseCommentViewHolder(View itemView) {
        super(itemView);
        mView = itemView;
        mContext = itemView.getContext();

        itemView.setOnClickListener(this);
    }

    public void bindComment(Comment comment){
        TextView usernameTextView = (TextView)mView.findViewById(R.id.comment_username);
        TextView comment_textview = (TextView) mView.findViewById(R.id.comment_textview);
        ImageButton moreOptionsImageButton = (ImageButton) mView.findViewById(R.id.comment_more_options);

        mphotoUserID = comment.getCommenter();
        mUrl = comment.getPhoto_url().replace(".webp", "");

        //usernameTextView.setText(comment.getCommenter());
        setCommentorsName(comment.getCommenter(), usernameTextView);
        comment_textview.setText(comment.getCommentString());
        moreOptionsImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext, "Set up more options", Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void setCommentorsName(String uid, final TextView usernameTextView) {
        FirebaseDatabase.getInstance().getReference(FirebaseConstants.USERDATA).child(uid).child(FirebaseConstants.USERNAME)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.getValue() != null) {
                            usernameTextView.setText(dataSnapshot.getValue().toString());

                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        usernameTextView.setText("BOB");
                    }
                });
    }

    @Override
    public void onClick(View view) {
        final ArrayList<Comment> comments = new ArrayList<>();
//      Reference correct section of database below
        Toast.makeText(mContext, "Item Clicked", Toast.LENGTH_SHORT).show();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Photos")
                .child(mUrl).child("Comments");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    comments.add(snapshot.getValue(Comment.class));
                }

//                int itemPosition = getLayoutPosition();

//                Intent intent = new Intent(mContext, RestaurantDetailActivity.class);
//                intent.putExtra("position", itemPosition + "");
//                intent.putExtra("restaurants", Parcels.wrap(restaurants));
//
//                mContext.startActivity(intent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }


}
