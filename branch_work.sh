#!/bin/bash

# Output markdown file header
output="branch_last_working.md"
echo "| Branch | User | Last Worked On | Time Ago | Commit SHA | Commit Message |" > "branch_last_working.md"
echo "|--------|------|----------------|----------|------------|----------------|" >> "branch_last_working.md"

# Fetch latest info from remote
git fetch --all --quiet

# Get all branches (local and remote), excluding HEAD symbolic refs
branches=$(git for-each-ref --format='%(refname:short)' refs/heads/ refs/remotes/ | grep -vE 'HEAD$')

# Array to store branch info in the format:
#   commit_date|author|relative_time|commit_sha|commit_message|branch_name
declare -a branch_info=()

for branch in $branches; do
    # Get the last commit info: date (YYYY-MM-DD), author, relative time, commit SHA, and commit message
    info=$(git log -1 --pretty=format:"%ad|%an|%ar|%H|%s" --date=short "$branch" 2>/dev/null)
    
    if [ -n "$info" ]; then
        branch_info+=("$info|$branch")
    else
        # In case no commits are found (rare scenario)
        branch_info+=("No commits found|-|-|-|-|$branch")
    fi
done

# Sort branches by the "Last Worked On" date (field 1) in descending order.
# This assumes dates are in YYYY-MM-DD format.
sorted=$(for entry in "${branch_info[@]}"; do echo "$entry"; done | sort -t'|' -k1,1 -r)

# Declare an associative array to track seen commit SHAs
declare -A seen

# Append sorted info as Markdown rows, only including unique commit SHAs.
while IFS='|' read -r date author relative commit_sha commit_message branch_name; do
    # Check if we've already output this commit SHA (skip if yes)
    if [[ -n "$commit_sha" && -n "${seen[$commit_sha]}" ]]; then
        continue
    fi
    # Mark this commit SHA as seen (if available)
    if [[ -n "$commit_sha" ]]; then
        seen[$commit_sha]=1
    fi
    echo "| \`$branch_name\` | $author | $date | $relative | \`$commit_sha\` | $commit_message |" >> "branch_last_working.md"
done <<< "$sorted"

echo "✅ Done. Output written to branch_last_working.md"
