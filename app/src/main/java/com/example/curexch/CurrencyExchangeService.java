package com.example.curexch;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CurrencyExchangeService {
    @GET("fetch-one")
    Call<ExchangeRateResponse> getExchangeRate(
            @Query("from") String fromCurrency,
            @Query("to") String toCurrency,
            @Query("api_key") String apiKey);
}
