package com.example.curexch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private Map<String, BigDecimal> userBalances = new HashMap<>();
    private int exchangeCount = 0;
    private final BigDecimal COMMISSION_RATE = new BigDecimal("0.007"); // 0.7% commission


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        userBalances.put("EUR", new BigDecimal("1000"));
        Spinner currencyFromSpinner = findViewById(R.id.currency_from_spinner);
        Spinner currencyToSpinner = findViewById(R.id.currency_to_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.currencies, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencyFromSpinner.setAdapter(adapter);
        currencyToSpinner.setAdapter(adapter);
        displayAllUserBalances();
        Button submitButton = findViewById(R.id.submit_button);
        submitButton.setOnClickListener(v -> onSubmit());
    }

    private void displayAllUserBalances() {
        StringBuilder balanceBuilder = new StringBuilder();
        for (Map.Entry<String, BigDecimal> entry : userBalances.entrySet()) {
            String currency = entry.getKey();
            BigDecimal balance = entry.getValue();
            balanceBuilder.append(String.format(Locale.getDefault(), "%s: %s\n", currency, balance.toPlainString()));
        }
        TextView balanceTextView = findViewById(R.id.user_balance);
        balanceTextView.setText(balanceBuilder.toString());
    }
    private void updateBalanceDisplay (String currencyCode){
        TextView balanceTextView = findViewById(R.id.user_balance);
        BigDecimal balance = userBalances.getOrDefault(currencyCode, BigDecimal.ZERO);
        String balanceText = String.format(Locale.getDefault(), "Balance: %s %s", currencyCode, balance.toPlainString());
        balanceTextView.setText(balanceText);
    }

    private void onSubmit() {
        EditText exchangeAmountEditText = findViewById(R.id.exchange_amount);
        Spinner currencyFromSpinner = findViewById(R.id.currency_from_spinner);
        Spinner currencyToSpinner = findViewById(R.id.currency_to_spinner);

        String amountStr = exchangeAmountEditText.getText().toString();
        String fromCurrency = currencyFromSpinner.getSelectedItem().toString();
        String toCurrency = currencyToSpinner.getSelectedItem().toString();

        if (!amountStr.isEmpty()) {
            BigDecimal amount = new BigDecimal(amountStr);
            fetchExchangeRate(fromCurrency, toCurrency, amount);
        }
    }

    private void fetchExchangeRate(String fromCurrency, String toCurrency, BigDecimal amount) {
        CurrencyExchangeService service = RetrofitClientInstance.getRetrofitInstance().create(CurrencyExchangeService.class);
        Call<ExchangeRateResponse> call = service.getExchangeRate(fromCurrency, toCurrency, "6d7c2cbb14-68b7cb6d8e-s8qt62");

        call.enqueue(new Callback<ExchangeRateResponse>() {
            @Override
            public void onResponse(@NonNull Call<ExchangeRateResponse> call, @NonNull Response<ExchangeRateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BigDecimal exchangeRate = BigDecimal.valueOf(response.body().getResult().get(toCurrency));
                    BigDecimal exchangedAmount = CurrentExchanger.calculateExchangedAmount(amount, exchangeRate);
                    BigDecimal amountToExchange = amount;
                    BigDecimal currentBalance = userBalances.getOrDefault(fromCurrency, BigDecimal.ZERO);
                    if (exchangeCount > 4) {
                        amountToExchange = amount.add(amount.multiply(COMMISSION_RATE));
                    }
                    if (currentBalance.compareTo(amountToExchange) < 0) {
                        showInsufficientFundsMessage(fromCurrency);
                        return; // Abort the operation
                    }
                    updateUserBalance(fromCurrency, toCurrency, amountToExchange, exchangedAmount);
                    showTransactionSummary(fromCurrency, toCurrency, amount, exchangedAmount);
                    exchangeCount++;
                }
            }

            @Override
            public void onFailure(@NonNull Call<ExchangeRateResponse> call, @NonNull Throwable t) {
                // Handle failure
            }
        });
    }

    private void updateUserBalance(String fromCurrency, String toCurrency, BigDecimal subtractAmount, BigDecimal addAmount) {
        BigDecimal fromBalance = userBalances.getOrDefault(fromCurrency, BigDecimal.ZERO);
        fromBalance = Objects.requireNonNull(fromBalance).subtract(subtractAmount);
        userBalances.put(fromCurrency, fromBalance);

        BigDecimal toBalance = userBalances.getOrDefault(toCurrency, BigDecimal.ZERO);
        toBalance = Objects.requireNonNull(toBalance).add(addAmount);
        userBalances.put(toCurrency, toBalance);

        runOnUiThread(() -> {
            StringBuilder balanceBuilder = new StringBuilder("Balance:\n");
            for (Map.Entry<String, BigDecimal> entry : userBalances.entrySet()) {
                String currency = entry.getKey();
                BigDecimal balance = entry.getValue();
                balanceBuilder.append(String.format(Locale.getDefault(), "%s: %s\n", currency, balance.toPlainString()));
            }
            TextView balanceTextView = findViewById(R.id.user_balance);
            balanceTextView.setText(balanceBuilder.toString());
        });

    }

    private void showTransactionSummary(String fromCurrency, String toCurrency, BigDecimal amount, BigDecimal exchangedAmount) {
        String message;
        if (exchangeCount > 4) {
           message = String.format(Locale.getDefault(), "You have converted %s %s to %s %s. Commission Fee - %s %s.",
                    amount.stripTrailingZeros().toPlainString(), fromCurrency,
                    exchangedAmount.stripTrailingZeros().toPlainString(), toCurrency,
                    COMMISSION_RATE.multiply(amount).stripTrailingZeros().toPlainString(), fromCurrency);
        } else {
            message = String.format(Locale.getDefault(), "You have converted %s %s to %s %s.",
                    amount.stripTrailingZeros().toPlainString(), fromCurrency,
                    exchangedAmount.stripTrailingZeros().toPlainString(), toCurrency);
        }
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());
    }

    private void showInsufficientFundsMessage(String currency) {
        String message = "Insufficient " + currency + " funds in your account.";
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());
    }


}