# LeetCode Integration

## Overview
The DevCommand backend integrates with the public LeetCode GraphQL API to sync a user's LeetCode account statistics into their DevCommand profile. The synchronization operates by mapping a user's DevCommand account to their public LeetCode username and retrieving their profile data (such as their total solved problems by difficulty).

## Configuration
The integration is configured via the `application.yml` file under the `leetcode` prefix.

```yaml
leetcode:
  enabled: true
  base-url: https://leetcode.com
  timeout-seconds: 10
```

- `enabled`: Toggles the LeetCode synchronization feature. If set to `false`, endpoints to sync or link accounts will gracefully return errors indicating the feature is disabled. The application starts fine with it disabled.
- `base-url`: The target API URL for LeetCode. Defaults to `https://leetcode.com`.
- `timeout-seconds`: The connection and read timeout limit.

## API Endpoints

### Link LeetCode Account
- **Endpoint**: `POST /api/dsa/accounts/leetcode/link`
- **Auth Required**: Yes (Bearer Token)
- **Description**: Associates a given LeetCode username with the authenticated DevCommand user. Upon successful linking, an initial synchronization is performed.
- **Request Body**:
  ```json
  {
    "username": "your_leetcode_username"
  }
  ```

### Sync LeetCode Account
- **Endpoint**: `POST /api/dsa/accounts/leetcode/sync`
- **Auth Required**: Yes (Bearer Token)
- **Description**: Triggers a manual sync of the currently linked LeetCode account for the authenticated DevCommand user. Fetches the latest stats from LeetCode and updates the database.
- **Response**: Returns the updated `ExternalDsaAccount` entity along with its `LeetCodeStats`.

### View Connected Account
- **Endpoint**: `GET /api/dsa/accounts/leetcode`
- **Auth Required**: Yes (Bearer Token)
- **Description**: Retrieves the authenticated user's connected LeetCode account and their latest synchronized statistics.

## Limitations
1. **Public Profiles Only**: The integration utilizes the public LeetCode GraphQL API. Only accounts with public profiles can be fetched. Users with private profiles will face synchronization errors.
2. **Rate Limiting**: Because it connects to LeetCode's public API without specific application credentials or authentication headers, the server's IP may be subject to rate limiting by LeetCode. It is recommended to configure periodic syncing cautiously.
3. **No Granular Submissions**: The API currently tracks total solved problems by difficulty (Easy, Medium, Hard). It does not pull down individual submissions or problem titles automatically into the `DsaProblem` entity table. 
4. **Resiliency**: If a synchronization attempt fails (due to network failure or LeetCode rate limits), the external account status will transition to `SYNC_FAILED`, but any previously captured statistics will remain intact.
