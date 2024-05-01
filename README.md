# GitHub Analyzer

This is a Java application that analyzes a GitHub repository. It fetches data from a GitHub repository and performs two types of analysis:

1. Finds pairs of contributors who have worked on the same file.
2. Analyzes the contributions of contributors in the last week.

## Prerequisites

- Java 8 or higher
- Maven
- GitHub Personal Access Token

## How to Run
1. Clone the repository using `git clone https://github.com/RaresPopa04/GithubAnalyzer.git`.
2. Navigate to the cloned folder `cd GithubAnalyzer`.
3. Navigate to the `demo` directory using  `cd demo`.
4. Set your GitHub Personal Access Token as an environment variable named `GITHUB_TOKEN`. You can do this by using `echo GITHUB_TOKEN=your_github_token_here > .env` (do not forget to replace the token value).
5. Build the application using the command `mvn clean package`.
6. After a successful build, a `github-analyzer-1.0-SNAPSHOT.jar` file will be generated in the `target` directory.
7. Run the application using the command `java -cp "target/classes;target/dependency/*" example.App`.

#Usage instruction
The app runs only in the terminal.
1. The is prompted with a message, where he needs to insert the name of the owner of the repository, next step is to insert the name of the repository.
2. The users needs to choose between 2 options of analytics on the specified repository.
3. For the first option, the tool will output the top 10 most common modifications between pairs of users.
4. For the second option, for each of the contributors, it will output the number of lines written, commits, merge requests and the max length of a comment to a merge request in the last week.
