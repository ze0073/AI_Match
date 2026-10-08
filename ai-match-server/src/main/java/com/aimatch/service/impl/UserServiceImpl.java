package com.aimatch.service.impl;

import com.aimatch.model.entity.User;
import com.aimatch.service.UserService;
import com.aimatch.store.DataStores;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final DataStores stores;

    @Override
    public User findByPhone(String phone) {
        return stores.userStore.findOne(u -> phone.equals(u.getPhone()));
    }

    @Override
    public User findByEmail(String email) {
        return stores.userStore.findOne(u -> email.equals(u.getEmail()));
    }

    @Override
    public User findByUsername(String username) {
        return stores.userStore.findOne(u -> username.equals(u.getUsername()));
    }
}
