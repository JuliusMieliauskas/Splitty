package client.utils;

import commons.exceptions.FailedRequestException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class CurrencyUtils {

    /**
     * Gets exchange ratio between USD and given currency
     */
    public static Double getExchangeRate(String currency) throws FailedRequestException {
        Response response = ConfigUtils.client.target(ConfigUtils.getServerUrl())
                .path("api/exchange-rate/USD/" + currency).request(MediaType.APPLICATION_JSON).get();
        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        } 
        return response.readEntity(Double.class);
    }
}
