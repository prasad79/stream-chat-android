package io.getstream.chat.example.view.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.getstream.sdk.chat.StreamChat;
import com.getstream.sdk.chat.rest.core.Client;

import io.getstream.chat.example.LoginActivity;
import io.getstream.chat.example.databinding.FragmentProfileBinding;
import io.getstream.chat.example.utils.UserConfig;

public class ProfileFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentProfileBinding binding = FragmentProfileBinding.inflate(inflater, container, false);
        Client client = StreamChat.getInstance(getContext());
        binding.setUser(client.getUser());
        binding.btnLogOut.setOnClickListener(view -> logOut());

        return binding.getRoot();
    }

    public void logOut(){
        UserConfig.logout();
        Client client = StreamChat.getInstance(getContext());
        client.disconnect();

        Intent intent = new Intent(getContext(), LoginActivity.class);
        startActivity(intent);
        getActivity().finish();
    }
}
