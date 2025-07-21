package com.magic.magicwebsocket.client.base;

import java.io.IOException;

public abstract class Callback {
    public void onFailure(IOException ioException){}
    public void onResponse(String Body){}
}
