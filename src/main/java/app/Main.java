package app;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.json.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    private static String getData(String url) throws Exception {
        HttpURLConnection httpCon = (HttpURLConnection) new URL(url).openConnection();
        httpCon.addRequestProperty("Authorization","token TOKEN_HERE");
        BufferedReader dataIn = new BufferedReader(new InputStreamReader(httpCon.getInputStream())); // burası değişebilir
        return dataIn.lines().reduce("",String::concat);// burası değişebilir
    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Organization name: ");
        String orgName = scanner.nextLine();

        System.out.print("\nNumber of most forked repositories: ");
        int mostForked = scanner.nextInt();

        System.out.print("\nNumber of top contributors: ");
        int topContributors = scanner.nextInt();

        String orgURL = "https://api.github.com/orgs/" + orgName;
        String json = getData(orgURL);
        JSONObject jsonOrg = new JSONObject(json);

        String reposURL = jsonOrg.getString("repos_url");
        json = getData(reposURL);

        JSONArray jsonForks = new JSONArray(json);

        final Map<Integer,Integer> forks = new HashMap<>();
        for (int i=0; i<jsonForks.length(); i++) {
            forks.put(i, Integer.parseInt(jsonForks.getJSONObject(i).get("forks_count").toString()));
        }

        Map<Integer, Integer> sortedForks = forks.entrySet()
                .stream().sorted((Map.Entry.<Integer, Integer>comparingByValue().reversed()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, LinkedHashMap::new));

        BufferedWriter writerRepo = Files.newBufferedWriter(Paths.get(orgName+"_repos.csv"));
        CSVPrinter repoPrinter = new CSVPrinter(writerRepo, CSVFormat.DEFAULT.withHeader("Repository Name", "Fork Quantity", "Public URL"));

        BufferedWriter writerUser = Files.newBufferedWriter(Paths.get(orgName+"_users.csv"));
        CSVPrinter userPrinter = new CSVPrinter(writerUser, CSVFormat.DEFAULT.withHeader("Repository Name", "Username", "Contribution Quantity","Follower Quantity"));

        Iterator<Map.Entry<Integer, Integer>> iterator = sortedForks.entrySet().iterator();
        for(int i=0; i<mostForked && iterator.hasNext(); i++) {
            Map.Entry<Integer,Integer> indexValue = iterator.next();

            String repoName = jsonForks.getJSONObject(indexValue.getKey()).getString("name");
            String publicURL = jsonForks.getJSONObject(indexValue.getKey()).getString("html_url");

            repoPrinter.printRecord(repoName, indexValue.getValue(), publicURL);

            String contURL = jsonForks.getJSONObject(indexValue.getKey()).getString("contributors_url");
            json = getData(contURL);
            JSONArray jsonContributors = new JSONArray(json);

            for(int j=0; j<topContributors && j<jsonContributors.length(); j++) {
                String username = jsonContributors.getJSONObject(j).getString("login");
                int contQuantity = Integer.parseInt(jsonContributors.getJSONObject(j).get("contributions").toString());
                int followerQuantity;

                String followerURL = jsonContributors.getJSONObject(j).getString("followers_url");
                json = getData(followerURL);
                JSONArray jsonFollowers = new JSONArray(json);
                followerQuantity = jsonFollowers.length();
                userPrinter.printRecord(repoName, username, contQuantity, followerQuantity);
            }
        }
        userPrinter.flush();
        repoPrinter.flush();
    }
}