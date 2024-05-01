# GitHub Analyzer

This is a Java application that analyzes a GitHub repository. It fetches data from a GitHub repository and performs two types of analysis:

1. Finds pairs of contributors who have worked on the same file.
2. Analyzes the contributions of contributors in the last week.

## Prerequisites

- Java 8 or higher
- Maven
- GitHub Personal Access Token

## How to Run
1. Clone the repository using `git clone https://github.com/RaresPopa04/GithubAnalyzer.git`
2. Navigate to the `demo` directory using  `cd demo`
3. Set your GitHub Personal Access Token as an environment variable named `GITHUB_TOKEN`.
4. Build the application using the command `mvn clean package`.
5. After a successful build, a `github-analyzer-1.0-SNAPSHOT.jar` file will be generated in the `target` directory.
6. Run the application using the command `java -cp "target/classes;target/dependency/*" example.App`
