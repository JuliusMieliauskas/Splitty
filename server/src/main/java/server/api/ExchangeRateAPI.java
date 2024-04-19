package server.api;

import commons.exceptions.FailedExchangeRateConversionException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.CurrencyExchangeRate;

@RestController
@RequestMapping("/api/exchange-rate")
public class ExchangeRateAPI {
    /**
     * Endpoint to get the exchange rate between two currencies
     * @param from The currency from which the value is exchanged
     * @param to The currency to which the value is exchanged
     * @return A ResponseEntity containing a Double with the exchange rate in case of success
     */
    @GetMapping(path = { "/{from}/{to}" })
    public ResponseEntity<?> getExchangeRate(@PathVariable("from") String from, @PathVariable("to") String to) {
        try {
            return ResponseEntity.ok(CurrencyExchangeRate.exchangeRateFromTo(from, to));
        } catch (FailedExchangeRateConversionException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}