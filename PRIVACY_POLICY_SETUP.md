# Privacy Policy Setup Guide

## Play Store Requirement

Google Play Store **requires** a privacy policy for apps that use sensitive permissions, including:
- `READ_PHONE_STATE` (which your app uses)
- Location permissions
- Other sensitive device data

## Quick Setup

### Option 1: Use GitHub Pages (Free & Easy)

1. **Create a GitHub repository** (or use your existing one)
2. **Create a file** `privacy-policy.md` in the repository
3. **Copy the content** from `PRIVACY_POLICY.md` in this project
4. **Enable GitHub Pages:**
   - Go to repository Settings → Pages
   - Select source branch (usually `main`)
   - Save
5. **Your privacy policy URL will be:**
   ```
   https://[your-username].github.io/[repository-name]/privacy-policy.html
   ```
   Or if you put it in the root:
   ```
   https://[your-username].github.io/[repository-name]/PRIVACY_POLICY.md
   ```

### Option 2: Use Your Own Website

1. Host the privacy policy on your website
2. Make it publicly accessible
3. Use the direct URL in Play Console

### Option 3: Use a Privacy Policy Generator

- **Privacy Policy Generator:** https://www.privacypolicygenerator.info/
- **Termly:** https://termly.io/products/privacy-policy-generator/
- **FreePrivacyPolicy:** https://www.freeprivacypolicy.com/

## Steps to Add Privacy Policy to Play Store

1. **Go to Google Play Console**
2. **Select your app**
3. **Navigate to:** Policy → App content
4. **Find:** "Privacy Policy" section
5. **Enter your privacy policy URL**
6. **Save**

## Important Notes

### Before Publishing:

1. **Update the Privacy Policy:**
   - Replace `[Your email address]` with your actual contact email
   - Update GitHub link if different
   - Customize any sections as needed

2. **Make it Accessible:**
   - Must be publicly accessible (no login required)
   - Should be a direct link (not behind a paywall)
   - Must be in a language your users understand

3. **Keep it Updated:**
   - Update when you add new features
   - Update when you change data collection practices
   - Keep the "Last Updated" date current

### What Play Store Checks:

- ✅ URL is accessible
- ✅ Privacy policy mentions the permissions you use
- ✅ Explains what data is collected
- ✅ Explains how data is used
- ✅ Explains data storage and sharing practices

## Template Customization

The provided `PRIVACY_POLICY.md` template includes:

- ✅ Explanation of READ_PHONE_STATE permission
- ✅ Location data collection explanation
- ✅ Data storage (local only)
- ✅ No third-party sharing
- ✅ User rights and data deletion
- ✅ Contact information placeholder

**You need to:**
1. Replace `[Your email address]` with your actual email
2. Verify the GitHub link is correct
3. Review and customize as needed for your specific use case

## Verification

After adding your privacy policy URL to Play Console:

1. Play Store will verify the URL is accessible
2. Review may take a few hours
3. You'll see a checkmark when approved

## Troubleshooting

**"Privacy policy URL is not accessible"**
- Make sure the URL is publicly accessible
- Test the URL in an incognito/private browser window
- Ensure no login is required

**"Privacy policy doesn't mention required permissions"**
- Make sure your policy mentions READ_PHONE_STATE
- Make sure it explains location data collection
- Be specific about what data is collected

**"Privacy policy format not supported"**
- Use HTML, PDF, or plain text
- GitHub Pages serves Markdown as HTML automatically
- Avoid Word documents or other proprietary formats

## Example URLs

If using GitHub Pages with your existing repository:
```
https://lightsaber42.github.io/cellgps/PRIVACY_POLICY.html
```

Or create a dedicated `privacy-policy.md` file:
```
https://lightsaber42.github.io/cellgps/privacy-policy.html
```

## Next Steps

1. ✅ Customize `PRIVACY_POLICY.md` with your contact info
2. ✅ Host it online (GitHub Pages recommended)
3. ✅ Add URL to Play Console
4. ✅ Wait for verification
5. ✅ Resubmit your app
