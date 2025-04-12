#!/bin/bash

# Output markdown file
echo "| Branch | User | Last Worked On |" > branch_last_working.md
echo "|--------|------|----------------|" >> branch_last_working.md

# Fetch latest info from remote
git fetch --all --quiet

# Get all branches (local + remote), ignore HEAD symbolic refs
branches=$(git for-each-ref --format='%(refname:short)' refs/heads/ refs/remotes/ | grep -vE 'HEAD$')

for branch in $branches; do
    # Get latest commit author and date
    latest_info=$(git log -1 --pretty=format:'%an | %ad' --date=short "$branch" 2>/dev/null)

    if [ -n "$latest_info" ]; then
        echo "| \`$branch\` | $latest_info |" >> branch_last_working.md
    else
        echo "| \`$branch\` | No commits found | - |" >> branch_last_working.md
    fi
done

echo "✅ Done. Output written to branch_last_working.md"
