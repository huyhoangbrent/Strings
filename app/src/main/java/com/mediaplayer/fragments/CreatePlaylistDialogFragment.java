package com.mediaplayer.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.mediaplayer.R;
import com.mediaplayer.adapters.PlaylistsAdapter;
import com.mediaplayer.beans.Playlist;
import com.mediaplayer.dao.MediaplayerDAO;
import com.mediaplayer.utilities.MediaLibraryManager;
import com.mediaplayer.utilities.MediaPlayerConstants;
import com.mediaplayer.utilities.MessageConstants;
import com.mediaplayer.utilities.SQLConstants;

public class CreatePlaylistDialogFragment extends DialogFragment {
    private Context context;
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        context = getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_create_playlist, null);
        Dialog playlistDialog = null;
        String tag = getTag();

        if(tag.equals(MediaPlayerConstants.TAG_CREATE_PLAYLIST)) {
            //Setting values for dialog window
            builder.setTitle(MediaPlayerConstants.TITLE_CREATE_PLAYLIST);
            builder.setView(dialogView);
            builder.setPositiveButton(MediaPlayerConstants.CREATE, null);
            playlistDialog = builder.create();

            playlistDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    final AlertDialog alertDialog = (AlertDialog) dialog;
                    Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);

                    //Setting on-click listener for positive button
                    positiveButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            //Retreiving playlist name entered by the user
                            EditText playlistTitleTextBox = (EditText) dialogView.findViewById(R.id.playlistTitle);
                            String playlistName = playlistTitleTextBox.getText().toString();

                            //Validating playlist title
                            if(isPlaylistTitleValid(playlistTitleTextBox)) {
                                //Setting the values for newly created playlist
                                Playlist playlist = new Playlist();
                                playlist.setPlaylistName(playlistName);
                                playlist.setPlaylistSize(SQLConstants.ZERO);
                                playlist.setPlaylistDuration(SQLConstants.ZERO);

                                MediaplayerDAO dao = new MediaplayerDAO(getContext());
                                dao.createPlaylist(playlist);

                                //Sorting the playlists
                                MediaLibraryManager.sortPlaylists();

                                //Updating list view adapter
                                updatePlaylistsAdapter();

                                //Dismissing the dialog window
                                alertDialog.dismiss();
                            }
                        }
                    });
                }
            });
        } else if(tag.equals(MediaPlayerConstants.TAG_RENAME_PLAYLIST)) {
            //Fetching old playlist values
            Bundle args = getArguments();
            final String oldPlaylistTitle = args.getString(MediaPlayerConstants.KEY_PLAYLIST_TITLE);
            final int oldPlaylistIndex = args.getInt(MediaPlayerConstants.KEY_PLAYLIST_INDEX);

            //Setting old playlist title in the input text box
            final EditText playlistTitleTextBox = (EditText) dialogView.findViewById(R.id.playlistTitle);
            playlistTitleTextBox.setText(oldPlaylistTitle);

            //Setting values for dialog window
            builder.setTitle(MediaPlayerConstants.TITLE_RENAME_PLAYLIST);
            builder.setView(dialogView);
            builder.setPositiveButton(MediaPlayerConstants.RENAME, null);
            playlistDialog = builder.create();

            playlistDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    final AlertDialog alertDialog = (AlertDialog) dialog;
                    Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);

                    //Setting on-click listener for positive button
                    positiveButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            //Retreiving playlist name entered by the user
                            String newPlaylistTitle = playlistTitleTextBox.getText().toString();

                            //Validating playlist title and checking if it is not the same as the old one
                            if(isPlaylistTitleValid(playlistTitleTextBox) && !newPlaylistTitle.equals(oldPlaylistTitle)) {
                                //Fetching selected playlist from playlistInfoList and updating playlist title
                                Playlist playlist = MediaLibraryManager.getPlaylistByIndex(oldPlaylistIndex);
                                playlist.setPlaylistName(newPlaylistTitle);

                                //Updating playlistInfoList with new playlist values
                                MediaLibraryManager.updatePlaylistInfoList(oldPlaylistIndex, playlist);

                                //Sort playlistInfoList to update the indices of the playlists
                                MediaLibraryManager.sortPlaylists();

                                //Getting upated playlistIndex of the renamed playlist
                                playlist = MediaLibraryManager.getPlaylistByTitle(newPlaylistTitle);
                                int newPlaylistIndex = playlist.getPlaylistIndex();
                                playlist.setPlaylistIndex(newPlaylistIndex);

                                //Updating table 'Playlist' with new values of playlist_title and playlist_index
                                MediaplayerDAO dao = new MediaplayerDAO(getContext());
                                dao.renamePlaylist(playlist);

                                //Dismissing the dialog window
                                alertDialog.dismiss();

                                // TODO: 27-Aug-16 Refresh Playlist Fragment
                            }
                        }
                    });
                }
            });
        }

        return playlistDialog;
    }

    private boolean isPlaylistTitleValid(EditText playlistTitleTextBox) {
        String newPlaylistTitle = playlistTitleTextBox.getText().toString();

        //Checking if playlist title is not empty string
        if(newPlaylistTitle.isEmpty()) {
            playlistTitleTextBox.setError(MessageConstants.ERROR_PLAYLIST_TITLE_BLANK);
            return false;
        }

        //Checking if playlist title is not the same as default playlist 'Favourites'
        else if(newPlaylistTitle.equalsIgnoreCase(SQLConstants.PLAYLIST_TITLE_FAVOURITES)) {
            playlistTitleTextBox.setError(MessageConstants.ERROR_PLAYLIST_TITLE_FAVOURITES);
            return false;
        }

        //Checking if playlist title is not the same as an existing playlist
        else if(MediaLibraryManager.getPlaylistByTitle(newPlaylistTitle) != null) {
            playlistTitleTextBox.setError(MessageConstants.ERROR_PLAYLIST_TITLE);
            return false;
        }
        else {
            return true;
        }
    }

    private void updatePlaylistsAdapter() {
        PlaylistsAdapter adapter = new PlaylistsAdapter(context, MediaLibraryManager.getPlaylistInfoList());
        ListView listView = PlaylistsFragment.listView;
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }
}
