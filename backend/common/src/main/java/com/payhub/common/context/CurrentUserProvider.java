package com.payhub.common.context;

public interface CurrentUserProvider {

    Object getUserByUsername(String username);
}
