# Version Management Guide

## How Releases Work

This project uses **automated releases** via GitHub Actions. Every push to `main` triggers a build and potentially creates a new release.

## Version Format

We use **Semantic Versioning** (SemVer): `MAJOR.MINOR.PATCH`

- **MAJOR** (1.x.x): Breaking changes, incompatible API changes
- **MINOR** (x.1.x): New features, backward-compatible
- **PATCH** (x.x.1): Bug fixes, backward-compatible

Example: `1.0.0` → `1.0.1` → `1.1.0` → `2.0.0`

---

## How to Release a New Version

### 1. Update Version Number

Edit `gradle.properties` and change the `mod_version`:

```properties
mod_version=1.0.0  # Change this number
```

### 2. Commit and Push

```bash
git add gradle.properties
git commit -m "Bump version to 1.0.0"
git push origin main
```

### 3. Automated Process

GitHub Actions will automatically:
1. Build the mod with Gradle
2. Check if tag `v1.0.0` exists
3. Create the tag if it doesn't exist
4. Generate a changelog from git commits
5. Create a GitHub Release with the JAR file

---

## Version Increment Guidelines

### When to increment PATCH (x.x.1)
- Bug fixes
- Performance improvements
- Small tweaks
- Documentation updates (if significant)

**Example changes:**
- Fixed webcam freeze on disconnect
- Improved connection error handling
- Updated README

**Version change:** `1.0.0` → `1.0.1`

### When to increment MINOR (x.1.0)
- New features
- New commands
- Enhanced functionality
- New configuration options

**Example changes:**
- Added new `/webcam stats` command
- Added web viewer
- Added room support
- Added chat notifications

**Version change:** `1.0.0` → `1.1.0`

### When to increment MAJOR (2.0.0)
- Breaking changes
- Minecraft version update
- Complete rewrites
- Removed features
- Changed configuration format

**Example changes:**
- Updated to Minecraft 1.22
- Changed from WebRTC to Socket.IO (breaking API)
- Removed old panel rendering mode

**Version change:** `1.5.2` → `2.0.0`

---

## Example Release Workflow

### Scenario: Adding a new feature

1. **Develop the feature** on a branch
   ```bash
   git checkout -b feature/add-quality-settings
   # ... make changes ...
   git commit -m "Add webcam quality settings"
   ```

2. **Merge to main**
   ```bash
   git checkout main
   git merge feature/add-quality-settings
   ```

3. **Update version** (this is a new feature, so MINOR bump)
   ```bash
   # Edit gradle.properties: 1.0.0 → 1.1.0
   git add gradle.properties
   git commit -m "Bump version to 1.1.0"
   ```

4. **Push to trigger release**
   ```bash
   git push origin main
   ```

5. **Check GitHub Actions**
   - Go to your repository → Actions tab
   - Watch the "Build and Release" workflow
   - Once complete, check Releases tab for new release

---

## Skipping Releases

If you want to push changes **without creating a release**:

1. **Don't change the version number** in `gradle.properties`
2. GitHub Actions will still build but **skip release creation** because the tag already exists

This is useful for:
- Documentation updates
- README changes
- Minor non-code changes

---

## Manual Release Trigger

You can manually trigger a build without pushing:

1. Go to GitHub → Actions tab
2. Select "Build and Release" workflow
3. Click "Run workflow"
4. Select branch and run

---

## Changelog Generation

The workflow automatically generates a changelog from git commits between releases.

**Write good commit messages!** They appear in the release notes.

**Good commit messages:**
```
✅ Add webcam quality settings
✅ Fix connection error on server restart
✅ Improve chat notification formatting
```

**Bad commit messages:**
```
❌ fix stuff
❌ update
❌ wip
```

---

## Current Version

Check the current version:
```bash
grep "mod_version" gradle.properties
```

Or look at the latest release on GitHub.

---

## Troubleshooting

### Build fails
- Check GitHub Actions logs for errors
- Ensure `./gradlew build` works locally
- Verify Java 21 is configured correctly

### Release not created
- Check if tag already exists for this version
- Verify `gradle.properties` was updated
- Ensure you pushed to `main` branch

### Wrong JAR file uploaded
- Workflow automatically finds the main JAR
- Excludes `-sources.jar` and `-dev.jar`
- Check "Find JAR file" step in Actions logs

---

## Quick Reference

| Change Type | Version Bump | Example |
|-------------|--------------|---------|
| Bug fix | PATCH | 1.0.0 → 1.0.1 |
| New feature | MINOR | 1.0.0 → 1.1.0 |
| Breaking change | MAJOR | 1.5.2 → 2.0.0 |

---

## Next Steps

After releasing:
1. ✅ Test the released JAR from GitHub Releases
2. ✅ Update documentation if needed
3. ✅ Announce the release (Discord, CurseForge, Modrinth, etc.)
