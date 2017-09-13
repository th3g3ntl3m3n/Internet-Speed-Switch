package me.th3g3ntl3m3n.energyswitch;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Created by hawkscode on 3/8/17.
 */

public interface IpInterface {

    @GET("/")
    Call<IPAddress> getIPAddress();
}
