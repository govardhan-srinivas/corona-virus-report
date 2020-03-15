package com.cornonavirus.report.services;

import com.cornonavirus.report.models.LocationStats;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class CoronoVirusDataService {
    private static String VIRUS_DATA_URL ="https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_time_series/time_series_19-covid-Confirmed.csv";
    private List<LocationStats> stats = new ArrayList<LocationStats>();

    public List<LocationStats> getAllStats(){
        return stats;
    }
    @PostConstruct
    @Scheduled(cron = "* * 1 * * *")
    private void fetchVirusData() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(VIRUS_DATA_URL)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        StringReader reader = new StringReader(response.body());
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);

        // Would show wrong info when method is running, So create a new List
        List<LocationStats> newStats = new ArrayList<LocationStats>();

        for (CSVRecord record : records) {
            LocationStats locationStat = new LocationStats();
            locationStat.setCountry(record.get("Province/State"));
            locationStat.setState(record.get("Country/Region"));
            int latestTotalCases = Integer.parseInt(record.get(record.size()-1));
            locationStat.setLatestTotalCases(latestTotalCases);
            int prevDayCases = Integer.parseInt(record.get(record.size()-2));
            locationStat.setDiffFromPrevDay(latestTotalCases - prevDayCases);
//            System.out.println(locationStat.toString());
            newStats.add(locationStat);
        }
        // Sorting
        Collections.sort(newStats, new Comparator<LocationStats>() {
            @Override
            public int compare(LocationStats stat2, LocationStats stat1)
            {
                return  Integer.compare(stat1.getDiffFromPrevDay() , stat2.getDiffFromPrevDay());
            }
        });
        // Replace the values with updated data
        this.stats = newStats;
    }
}
