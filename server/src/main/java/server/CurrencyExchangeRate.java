package server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import commons.exceptions.FailedExchangeRateConversionException;
import commons.exceptions.InvalidFileFormatException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.time.LocalDate;


public class CurrencyExchangeRate {


    /**
     * Calculates the exchange rate between two currencies.
     *
     * @param fromCurrency the currency that toCurrency is getting converted from.
     * @param toCurrency   the currency that fromCurrency is getting converted to.
     * @return the exchange rate from fromCurrency to toCurrency.
     * @throws FailedExchangeRateConversionException if API request can't be made
     */
    public static double exchangeRateFromTo(String fromCurrency, String toCurrency) throws FailedExchangeRateConversionException {
        if (fromCurrency == null || toCurrency == null) {
            throw new FailedExchangeRateConversionException("Currencies cannot be null!");
        }
        fromCurrency = fromCurrency.toUpperCase();
        toCurrency = toCurrency.toUpperCase();
        if (fromCurrency.equals(toCurrency)) {
            return 1.0;
        }

        File file = getFile(fromCurrency, toCurrency, false);
        try {
            if (file == null) {
                throw new InvalidFileFormatException("File does not exist");
            }
            return readFromFile(file);
        } catch (InvalidFileFormatException e) {
            double exchangeRate = currencyApiRequest(fromCurrency, toCurrency);

            // only try to create file after exchangeRate request is successful
            file = getFile(fromCurrency, toCurrency, true);
            try {
                writeToFile(exchangeRate, file);
            } catch (FileNotFoundException ex) {
                // Should not happen since file was created in getFile
                throw new RuntimeException(ex);
            }
            return exchangeRate;
        }
    }

    /**
     * Creates the file path where the currency exchange rate must be located.
     *
     * @param fromCurrency the currency that toCurrency is getting converted from.
     * @param createIfNotExists create the file if it does not exist, if false the method will return null if it doesn't exist
     * @return the file path where the currency exchange rate must be located.
     */
    public static File getFile(String fromCurrency, String toCurrency, boolean createIfNotExists) {
        String currentDate = LocalDate.now().toString();
        String path = "rates/" + currentDate + "/" + fromCurrency + "/" + toCurrency + ".txt";
        File file = new File(path);
        if (!file.exists()) {
            if (!createIfNotExists) {
                return null;
            }

            try {
                File parent = file.getParentFile();
                if ((!parent.exists() && !parent.mkdirs()) | !file.createNewFile()) { // Try to create all parent folders and file
                    throw new RuntimeException();
                }
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        }
        return file;
    }

    /**
     * Retrieves the exchange rate between two currencies using a specified API and passes them to another method to be read.
     *
     * @param fromCurrency the currency that toCurrency is getting converted from.
     * @param toCurrency   the currency that fromCurrency is getting converted to.
     * @return the exchange rate from fromCurrency to toCurrency.
     * @throws FailedExchangeRateConversionException if API request can't be reached.
     */
    protected static double currencyApiRequest(String fromCurrency, String toCurrency) throws FailedExchangeRateConversionException {
        StringBuilder response = new StringBuilder();
        String apiKey = "fca_live_rYWblLf6DXWUcTWq6Q1rZlgxc3pTxW5GJQgqBnSL";
        String exchangeURL = "https://api.freecurrencyapi.com/v1/latest?apikey=" +
                apiKey + "&currencies=" + fromCurrency + "," + toCurrency;
        URL url;
        try {
            url = new URL(exchangeURL);
        } catch (MalformedURLException e) {
            throw new FailedExchangeRateConversionException("Invalid currencies");
        }

        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) url.openConnection();
            con.setInstanceFollowRedirects(true);
            con.setRequestMethod("GET");
            int status = con.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new FailedExchangeRateConversionException("API request failed (status" + status + ")\n" + con.getErrorStream());
            }

            Scanner scanner = new Scanner(con.getInputStream());
            while (scanner.hasNext()) {
                response.append(scanner.nextLine());
            }

        } catch (IOException e) {
            if (con != null) {
                con.disconnect();
            }
            throw new FailedExchangeRateConversionException("Error when trying to create API request:\n" + e.getMessage());
        }

        String responseString = response.toString();
        try {
            return fromJSON(responseString, fromCurrency, toCurrency);
        } catch (JsonProcessingException | FailedExchangeRateConversionException e) {
            throw new FailedExchangeRateConversionException("Invalid response:\n" + responseString + "\n\n" + e.getMessage());
        }
    }

    /**
     * Parses the API response to extreact the exchange rate between two currencies
     *
     * @param responseString the JSON response from the API GET request.
     * @param fromCurrency   the currency that toCurrency is getting converted from.
     * @param toCurrency     the currency that fromCurrency is getting converted to.
     * @return the exchange rate from fromCurrency to toCurrency.
     * @throws JsonProcessingException if an error occurs while parsing the JSON response.
     * @throws FailedExchangeRateConversionException if the currencies are not available in the JSON response
     */
    public static double fromJSON(String responseString, String fromCurrency, String toCurrency)
            throws JsonProcessingException, FailedExchangeRateConversionException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(responseString);
        JsonNode dataNode = rootNode.path("data");
        if (!dataNode.has(toCurrency) || !dataNode.has(fromCurrency)) {
            throw new FailedExchangeRateConversionException("Exchange rate data for one or both currencies is unavailable.");
        }
        double fromCurrencyValue = dataNode.path(fromCurrency).asDouble();
        double toCurrencyValue = dataNode.path(toCurrency).asDouble();
        return toCurrencyValue / fromCurrencyValue;
    }

    /**
     * Writes the exchange rate to a txt file (creates directories if necessary).
     *
     * @param exchangeRate the exchange rate from fromCurrency to toCurrency
     * @param file The file to write to
     * @throws FileNotFoundException if writing to the file was not successful
     */
    protected static void writeToFile(double exchangeRate, File file) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(file);
        writer.println(exchangeRate);
        writer.flush();
        writer.close();
    }

    /**
     * Reads the exchange rate from a file
     *
     * @param file The file that the exchange rate is going to be read from
     * @return the exchange rate
     * @throws InvalidFileFormatException if the information in the file is not in the correct format
     */
    public static double readFromFile(File file) throws InvalidFileFormatException {
        try (Scanner scanner = new Scanner(file)) {
            double exchangeRate = Double.parseDouble(scanner.nextLine());
            scanner.close();
            return exchangeRate;
        } catch (NumberFormatException e) {
            throw new InvalidFileFormatException("File does not contain a double");
        } catch (FileNotFoundException e) {
            throw new InvalidFileFormatException("File not found");
        } catch (NoSuchElementException e) {
            throw new InvalidFileFormatException("File is empty");
        }
    }
}
