import java.util.*;


import org.json.JSONArray;
import org.springframework.http.client.reactive.*;

import org.json.JSONObject;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.File;

public class Main {
    public static void main(String[] args) {

        try{

            String apiFilePath;
            String credsFilePath;

            if(args.length < 2){
                apiFilePath = "C:\\Users\\dan_v\\OneDrive\\Desktop\\cfg\\api.txt";
                credsFilePath = "C:\\Users\\dan_v\\OneDrive\\Desktop\\cfg\\emailcreds.txt";
            }
            else {
                apiFilePath = args[0];
                credsFilePath = args[1];
            }

            File apiFile = new File(apiFilePath);
            File credsFile = new File(credsFilePath);

            if(!apiFile.exists() || !credsFile.exists()){
                return;
            }

            Scanner apiScanner = new Scanner(apiFile);
            String apiPath = apiScanner.nextLine();
            ArrayList<String> recipients = new ArrayList<String>();

            while(apiScanner.hasNextLine()){
                recipients.add(apiScanner.nextLine());
            }
            SendReport(apiPath, recipients, credsFile.getPath());
        }
        catch(Exception e){
            System.out.println(e.toString());
        }
    }

    private static void SendReport(String apiPath, ArrayList<String> recipients, String emailCredPath) {
        WebClient.Builder builder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(true)
                ));
        WebClient client = builder.build();
        String responseJson_string = client.get()
                .uri(apiPath)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JSONObject jsonObject = new JSONObject(responseJson_string);

        String forecastJson_string = client.get()
                .uri(jsonObject.getJSONObject("properties").get("forecast").toString())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JSONObject forecastJson = new JSONObject(forecastJson_string);

        JSONArray periodsArray = forecastJson.getJSONObject("properties").getJSONArray("periods");
        //PrintJSONNames(periodsJson);

        int numPeriods = periodsArray.length();

        StringBuilder emailBody = new StringBuilder("Hello! Here is the seven-day forecast for Forest Hills, NY.<br><br>");

        for(int i = 0; i < numPeriods; i++){
            JSONObject subjectPeriod = periodsArray.getJSONObject(i);
            emailBody.append("<b><u>").append(subjectPeriod.get("name")).append("</u></b><br>");
            emailBody.append(subjectPeriod.get("detailedForecast")).append("<br><br>");
        }
        emailBody.append("Enjoy your day!");
        Emailer emailer = new Emailer(emailCredPath);
        emailer.SendMail(recipients, "Daily Weather Report", emailBody.toString(), "dan@danielvalle.net");
    }
    private static void PrintJSONNames(JSONObject json){
        JSONArray names = json.names();
        for(int i = 0; i < names.length(); i++){
            System.out.println(names.get(i).toString());
        }
    }
}