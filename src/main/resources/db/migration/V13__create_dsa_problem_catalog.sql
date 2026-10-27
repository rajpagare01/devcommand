CREATE TABLE dsa_problem_catalog (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL UNIQUE,
    normalized_title VARCHAR(255) NOT NULL,
    platform VARCHAR(50) NOT NULL,
    difficulty VARCHAR(50) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    problem_url VARCHAR(2048),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dsa_catalog_normalized_title ON dsa_problem_catalog(normalized_title);
CREATE INDEX idx_dsa_catalog_platform ON dsa_problem_catalog(platform);

-- Seed authoritative catalog with common DSA problems
INSERT INTO dsa_problem_catalog (title, normalized_title, platform, difficulty, topic, problem_url) VALUES
('Two Sum', 'twosum', 'LEETCODE', 'EASY', 'Arrays & Hashing', 'https://leetcode.com/problems/two-sum/'),
('3Sum', '3sum', 'LEETCODE', 'MEDIUM', 'Two Pointers', 'https://leetcode.com/problems/3sum/'),
('Binary Search', 'binarysearch', 'LEETCODE', 'EASY', 'Binary Search', 'https://leetcode.com/problems/binary-search/'),
('Two Sum II - Input Array Is Sorted', 'twosumiiinputarrayissorted', 'LEETCODE', 'MEDIUM', 'Two Pointers', 'https://leetcode.com/problems/two-sum-ii-input-array-is-sorted/'),
('Two Sum IV - Input is a BST', 'twosumivinputisabst', 'LEETCODE', 'EASY', 'Trees', 'https://leetcode.com/problems/two-sum-iv-input-is-a-bst/'),
('Valid Anagram', 'validanagram', 'LEETCODE', 'EASY', 'Arrays & Hashing', 'https://leetcode.com/problems/valid-anagram/'),
('Contains Duplicate', 'containsduplicate', 'LEETCODE', 'EASY', 'Arrays & Hashing', 'https://leetcode.com/problems/contains-duplicate/'),
('Group Anagrams', 'groupanagrams', 'LEETCODE', 'MEDIUM', 'Arrays & Hashing', 'https://leetcode.com/problems/group-anagrams/'),
('Top K Frequent Elements', 'topkfrequentelements', 'LEETCODE', 'MEDIUM', 'Arrays & Hashing', 'https://leetcode.com/problems/top-k-frequent-elements/'),
('Product of Array Except Self', 'productofarrayexceptself', 'LEETCODE', 'MEDIUM', 'Arrays & Hashing', 'https://leetcode.com/problems/product-of-array-except-self/'),
('Valid Palindrome', 'validpalindrome', 'LEETCODE', 'EASY', 'Two Pointers', 'https://leetcode.com/problems/valid-palindrome/'),
('Container With Most Water', 'containerwithmostwater', 'LEETCODE', 'MEDIUM', 'Two Pointers', 'https://leetcode.com/problems/container-with-most-water/'),
('Trapping Rain Water', 'trappingrainwater', 'LEETCODE', 'HARD', 'Two Pointers', 'https://leetcode.com/problems/trapping-rain-water/'),
('Best Time to Buy and Sell Stock', 'besttimetobuyandsellstock', 'LEETCODE', 'EASY', 'Sliding Window', 'https://leetcode.com/problems/best-time-to-buy-and-sell-stock/'),
('Longest Substring Without Repeating Characters', 'longestsubstringwithoutrepeatingcharacters', 'LEETCODE', 'MEDIUM', 'Sliding Window', 'https://leetcode.com/problems/longest-substring-without-repeating-characters/'),
('Valid Parentheses', 'validparentheses', 'LEETCODE', 'EASY', 'Stack', 'https://leetcode.com/problems/valid-parentheses/'),
('Min Stack', 'minstack', 'LEETCODE', 'MEDIUM', 'Stack', 'https://leetcode.com/problems/min-stack/'),
('Reverse Linked List', 'reverselinkedlist', 'LEETCODE', 'EASY', 'Linked List', 'https://leetcode.com/problems/reverse-linked-list/'),
('Merge Two Sorted Lists', 'mergetwosortedlists', 'LEETCODE', 'EASY', 'Linked List', 'https://leetcode.com/problems/merge-two-sorted-lists/'),
('Linked List Cycle', 'linkedlistcycle', 'LEETCODE', 'EASY', 'Linked List', 'https://leetcode.com/problems/linked-list-cycle/'),
('Invert Binary Tree', 'invertbinarytree', 'LEETCODE', 'EASY', 'Trees', 'https://leetcode.com/problems/invert-binary-tree/'),
('Maximum Depth of Binary Tree', 'maximumdepthofbinarytree', 'LEETCODE', 'EASY', 'Trees', 'https://leetcode.com/problems/maximum-depth-of-binary-tree/'),
('Same Tree', 'sametree', 'LEETCODE', 'EASY', 'Trees', 'https://leetcode.com/problems/same-tree/'),
('Subtree of Another Tree', 'subtreeofanothertree', 'LEETCODE', 'EASY', 'Trees', 'https://leetcode.com/problems/subtree-of-another-tree/'),
('Search in Rotated Sorted Array', 'searchinrotatedsortedarray', 'LEETCODE', 'MEDIUM', 'Binary Search', 'https://leetcode.com/problems/search-in-rotated-sorted-array/'),
('Find Minimum in Rotated Sorted Array', 'findminimuminrotatedsortedarray', 'LEETCODE', 'MEDIUM', 'Binary Search', 'https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/'),
('Climbing Stairs', 'climbingstairs', 'LEETCODE', 'EASY', 'Dynamic Programming', 'https://leetcode.com/problems/climbing-stairs/'),
('Coin Change', 'coinchange', 'LEETCODE', 'MEDIUM', 'Dynamic Programming', 'https://leetcode.com/problems/coin-change/'),
('Longest Increasing Subsequence', 'longestincreasingsubsequence', 'LEETCODE', 'MEDIUM', 'Dynamic Programming', 'https://leetcode.com/problems/longest-increasing-subsequence/'),
('Number of Islands', 'numberofislands', 'LEETCODE', 'MEDIUM', 'Graphs', 'https://leetcode.com/problems/number-of-islands/'),
('Clone Graph', 'clonegraph', 'LEETCODE', 'MEDIUM', 'Graphs', 'https://leetcode.com/problems/clone-graph/'),
('Median of Two Sorted Arrays', 'medianoftwosortedarrays', 'LEETCODE', 'HARD', 'Binary Search', 'https://leetcode.com/problems/median-of-two-sorted-arrays/');
