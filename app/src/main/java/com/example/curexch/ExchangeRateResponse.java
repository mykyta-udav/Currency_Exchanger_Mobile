package com.example.curexch;

import java.util.Map;

public class ExchangeRateResponse {
    private String base;
    private Map<String, Double> result;
    private String updated;
    private int ms;

    // Getters and Setters
    public String getBase() { return base; }
    public void setBase(String base) { this.base = base; }
    public Map<String, Double> getResult() { return result; }
    public void setResult(Map<String, Double> result) { this.result = result; }
    public String getUpdated() { return updated; }
    public void setUpdated(String updated) { this.updated = updated; }
    public int getMs() { return ms; }
    public void setMs(int ms) { this.ms = ms; }
}
