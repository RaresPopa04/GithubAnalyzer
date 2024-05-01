package example;

import org.kohsuke.github.*;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class App {
    public static String GITHUB_TOKEN;

    static {
        
        try {
            Dotenv dotenv = Dotenv.load();
            GITHUB_TOKEN = dotenv.get("GITHUB_TOKEN");
        } catch (Exception e) {
            System.err.println("An error occurred while reading the environment variables: " + e.getMessage());
            System.exit(1);
        }
        
        if (GITHUB_TOKEN.isEmpty()) {
            System.err.println("Please set the GITHUB_TOKEN environment variable");
            System.exit(1);
        }
    }
    public static final long WEEK_IN_MILLIS = 7L * 24 * 3600 * 1000;

    public static void main(String[] args) throws IOException {
        System.out.println("GitHub Analyzer");
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Insert owner:");
            String owner = scanner.next();
            System.out.println("Insert repository:");
            String repository = scanner.next();

            GitHub github;
            
            try {
                github = new GitHubBuilder().withOAuthToken(GITHUB_TOKEN).build();
            } catch (IOException e) {
                System.err.println("An error occurred while accessing GitHub: " + e.getMessage());
                return;
            }

            GHRepository repo = findRepository(owner, repository,github);
            if (repo == null) {
                return;
            }

            System.out.println("What do you want to analyze?");
            System.out.println("1. Pair of contributors that worked on the same file");
            System.out.println("2. Contributions of contributors in the last week");
            int choice = scanner.nextInt();
            switch (choice) {
                case 1:
                    List<GHCommit> commits = analyzeRepository(github, repo, choice);
                    if(commits == null) {
                        System.err.println("An error occurred while analyzing the repository");
                        return;
                    }
                    analyzeContributorsPairs(repo, commits, commits.size());
                    break;
                case 2:
                    List<GHCommit> commits2 = analyzeRepository(github, repo, choice);
                    if(commits2 == null) {
                        System.err.println("An error occurred while analyzing the repository");
                        return;
                    }
                    Map<String, List<GHCommit>> commitsByAuthor = groupCommits(commits2,commits2.size());
                    Date weekAgo = new Date(System.currentTimeMillis() - WEEK_IN_MILLIS);
                    analyzeContributorsContributions(repo, commitsByAuthor, weekAgo, repo.listContributors().toList());
                    break;
                default:
                    System.out.println("Invalid choice");
                    break;
            }
        } catch (IOException e) {
            System.err.println("An error occurred while analyzing the repository: " + e.getMessage());
        }
    }

    /**
     * Finds a GitHub repository.
     * @param owner The owner of the repository
     * @param repository The repository name
     * @param github The GitHub instance
     * @return The found repository, or null if not found
     */

    private static GHRepository findRepository(String owner, String repository,GitHub github) {
        GHRepository repo;
        try {
            repo = github.getRepository(owner + "/" + repository);
            System.out.println("Repository found: " + repo.getFullName());
            return repo;
        } catch (GHFileNotFoundException e) {
            System.err.println("Repository not found");
            return null;
        } catch (IOException e) {
            System.err.println("An error occurred while accessing the repository: " + e.getMessage());
            return null;
        }
    }

    /**
     * Analyzes a GitHub repository and returns a list of commits.
     * @param github The GitHub instance
     * @param repo The repository to analyze
     * @param choice The choice of analysis
     * @return A list of commits in the repository
     * @throws IOException If an I/O error occurs
     */
    public static List<GHCommit> analyzeRepository(GitHub github, GHRepository repo, int choice) throws IOException {

        List<GHCommit> commits = new ArrayList<>();
        try {
            commits = repo.listCommits().withPageSize(100).toList();
            System.out.println("Commits: " + commits.size());
            return commits;

        } catch (IOException e) {
            System.out.println("An error occurred while accessing the repository: " + e.getMessage());
            return null;
        }
    }

    /**
     * Groups commits by author.
     * @param commits The list of commits
     * @param numOfCommits The number of commits
     * @return A map of commits grouped by author
     */
    private static Map<String,List<GHCommit>> groupCommits(List<GHCommit> commits,int numOfCommits){
        AtomicInteger counter = new AtomicInteger(0);
        Map<String, List<GHCommit>> commitsByAuthor = commits.stream()
                .filter(commit -> {
                    int count = counter.incrementAndGet();
                    int percentage = (int) (((double) count / numOfCommits) * 100);
                    getProgressBar(percentage);
                    try {
                        return commit.getAuthor() != null;
                    }catch (GHFileNotFoundException e) {
                        System.err.println("\nError with commit, skipping" + commit);
                        
                        return false;
                    } catch (IOException e) {
                        System.err.println("\nError with commit, skipping" + commit);
                        return false;
                    }

                }).collect(Collectors.groupingBy(commit -> {
                    try {
                        return commit.getAuthor().getLogin();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));
        return commitsByAuthor;
    }

    /**
     * Analyzes the contributions of contributors in the last week.
     * @param repo The repository to analyze
     * @param commitsByAuthor The commits grouped by author
     * @param weekAgo The date one week ago 
     * @param contributors The list of contributors
     * @throws IOException If an I/O error occurs
     */
    public static void analyzeContributorsContributions(GHRepository repo, Map<String, List<GHCommit>> commitsByAuthor, Date weekAgo, List<GHRepository.Contributor> contributors) throws IOException {
        System.out.println("\n\nAnalyzing contributors contributions");
        for (GHRepository.Contributor contributor : contributors) {
            List<GHCommit> commitsByThisAuthor = commitsByAuthor.getOrDefault(contributor.getLogin(), Collections.emptyList());
            analyzeContributor(repo, contributor, weekAgo, commitsByThisAuthor);
        }
    }


    /**
     * Analyzes the contributors pairs that worked on the same file.
     * @param repo The repository to analyze
     * @param commits The list of commits
     * @param numOfCommits The number of commits
     * @throws IOException If an I/O error occurs
     */
    private static void analyzeContributorsPairs(GHRepository repo, List<GHCommit> commits, int numOfCommits) throws IOException{
        Map<String, Set<String>> contributorsFileMap = new ConcurrentHashMap<>();
        System.out.println("\n\nAnalyzing contributors pairs");
        AtomicInteger counter = new AtomicInteger(0);
        commits.forEach((commit) -> parseCommit(commit, numOfCommits, contributorsFileMap,counter));
        displayResult(getTopPairs(contributorsFileMap));
    }

    /**
     * Analyzes a contributor from a repository.
     * @param repo The repository to analyze
     * @param contributor The contributor to analyze
     * @param weekAgo The date one week ago
     * @param commitsByThisAuthor The commits by this author
     * @throws IOException If an I/O error occurs
     */
    private static void analyzeContributor(GHRepository repo, GHRepository.Contributor contributor, Date weekAgo, List<GHCommit> commitsByThisAuthor) throws IOException {
        System.out.println("Analyzing contributor: " + contributor.getLogin());
        int linesOfCode = 0;
        int commitsNo;
        int mergeRequests;
        int maxCharsInComment = 0;

        List<GHCommit> commits = commitsByThisAuthor.stream()
                .filter(commit -> {
                    try {
                        return commit.getCommitDate().after(weekAgo);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        commitsNo = commits.size();

        for (GHCommit commit : commits) {
            for (GHCommit.File file : commit.listFiles()) {
                linesOfCode += file.getLinesAdded();
            }
        }

        List<GHPullRequest> pullRequests = repo.getPullRequests(GHIssueState.ALL)
                .stream().filter(ghPullRequest -> {
                    try {
                        return (ghPullRequest.getUser().getLogin().equals(contributor.getLogin()) && ghPullRequest.getCreatedAt().after(weekAgo));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
        mergeRequests = pullRequests.size();

        List<GHPullRequestReviewComment> comments = new ArrayList<>();
        pullRequests.stream().map(ghPullRequest -> {
            try {
                return ghPullRequest.listReviewComments();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).forEach(ghPullRequestReviewComments -> {
            try {
                comments.addAll(ghPullRequestReviewComments.toList());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        maxCharsInComment = comments.stream().
                map(GHPullRequestReviewComment::getBody).
                map(String::length).max(Integer::compareTo).orElse(0);

        System.out.println("Contributor: " + contributor.getLogin());
        System.out.println("Lines of code: " + linesOfCode);
        System.out.println("Commits: " + commitsNo);
        System.out.println("Merge requests: " + mergeRequests);
        System.out.println("Max chars in comment: " + maxCharsInComment);
        System.out.println("\n\n");
    }

    /**
     * Returns the top contributors pairs(max 10) that worked on the same file.
     * @param contributorsFileMap The contributors file map 
     * @return The top contributors pairs
     */

    private static List<Map.Entry<String, Integer>> getTopPairs(Map<String, Set<String>> contributorsFileMap) {
        Map<String, Integer> pairFrequency = new HashMap<>();
        for (Set<String> contributors : contributorsFileMap.values()) {
            List<String> contributorList = new ArrayList<>(contributors);
            for (int i = 0; i < contributorList.size(); i++) {
                for (int j = i + 1; j < contributorList.size(); j++) {
                    String pair = contributorList.get(i) + " - " + contributorList.get(j);
                    pairFrequency.put(pair, pairFrequency.getOrDefault(pair, 0) + 1);
                }
            }
        }

        return pairFrequency.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }
    
    /**
     * Displays the top contributors pairs
     * @param sortedPairs The sorted pairs of contributors to display
     */
    private static void displayResult(List<Map.Entry<String, Integer>> sortedPairs) {
        if (sortedPairs.isEmpty()) {
            System.out.println("\n\nNo contributors pairs found");
            return;
        }

        System.out.println("\n\nTop contributors pairs:");
        sortedPairs.stream().limit(10).forEach(pair -> {
            System.out.println(pair.getKey() + " : " + pair.getValue());
        });
    }

    /**
     * Parses a commit and updates the contributors file map.
     * @param commit The commit to parse
     * @param numOfCommits The number of commits
     * @param contributorsFileMap The contributors file map
     * @param counter The counter of commits parsed
     */
    private static void parseCommit(GHCommit commit, int numOfCommits, Map<String, Set<String>> contributorsFileMap,AtomicInteger counter) {
        int count = counter.incrementAndGet();
        int percentage = (int) (((double) count / numOfCommits) * 100);
        getProgressBar(percentage);
        try {
            GHUser author = null;
            try {
                author = commit.getAuthor();
            } catch (GHFileNotFoundException e) {
                System.err.println("\nError with commit, skipping: " + commit);
                return;
                
            }
            if (author != null) {
                String authorLogin = commit.getAuthor().getLogin();
                List<GHCommit.File> files = commit.listFiles().toList();
    
                for (GHCommit.File file : files) {
                    String filename = file.getFileName();
                    contributorsFileMap.computeIfAbsent(filename, k -> new HashSet<>()).add(authorLogin);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void getProgressBar(int percentage) {
        StringBuilder progressBar = new StringBuilder("[");
        for (int i = 0; i < 50; i++) {
            if (i < percentage / 2) {
                progressBar.append("=");
            } else {
                progressBar.append(" ");
            }
        }
        progressBar.append("] " + percentage + "%");
        System.out.print("\r" + progressBar.toString());
        System.out.flush();
    }
}
