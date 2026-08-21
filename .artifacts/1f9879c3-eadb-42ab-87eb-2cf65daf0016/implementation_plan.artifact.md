# Magic Cleaner & UI Polish Plan (v11.2)

This plan removes Cloud Sync, implements a new **Magic Cleaner** (Duplicate Finder) tool, enhances the Home Screen Scroll Hint, and fixes Share safety in history.

## User Review Required

> [!IMPORTANT]
> - **Cloud Sync**: Will be completely removed from the sidebar and success dialog.
> - **Magic Cleaner**: A new tool to find and delete duplicate/similar photos will be added in its place.
> - **Scroll Hint**: Will now be interactive (clickable) and reappear on scroll-up.

## Proposed Changes

### 1. Feature Replacement (Cloud Sync → Magic Cleaner)
#### [MODIFY] [activity_main.xml](file:///C:/Users/Aaditya Shukla/StudioProjects/MediaShrinker/app/src/main/res/layout/activity_main.xml) & [strings.xml](file:///C:/Users/Aaditya Shukla/StudioProjects/MediaShrinker/app/src/main/res/values/strings.xml)
- Remove `cloud_sync` and `cloud_emoji` strings.
- Add `magic_cleaner` and `magic_cleaner_emoji` (✨) strings.
- Update Sidebar: Replace `cloudSyncOption` with `magicCleanerOption`.
- Update Success Dialog: Remove the Cloud Export button.
#### [NEW] [activity_magic_cleaner.xml](file:///C:/Users/Aaditya Shukla/StudioProjects/MediaShrinker/app/src/main/res/layout/activity_magic_cleaner.xml)
- Layout for scanning and listing duplicate photos.
#### [NEW] [MagicCleanerActivity.kt](file:///C:/Users/Aaditya Shukla/StudioProjects/MediaShrinker/app/src/main/java/com/aaditya/mediashrinker/MagicCleanerActivity.kt)
- Implements a local perceptual hash algorithm to find similar photos.
- Fast background scanning and gallery cleanup logic.

### 2. Scroll Hint Enhancements
#### [MODIFY] [MainActivity.kt](file:///C:/Users/Aaditya Shukla/StudioProjects/MediaShrinker/app/src/main/java/com/aaditya/mediashrinker/MainActivity.kt)
- Update scroll listener to toggle hint visibility based on scroll position (`scrollY < 100`).
- Add `scrollHintLayout.setOnClickListener`: Perform a smooth scroll to the bottom.

### 3. History Share Safety
#### [MODIFY] [HistoryAdapter.kt](file:///C:/Users/Aaditya Shukla/StudioProjects/MediaShrinker/app/src/main/java/com/aaditya/mediashrinker/HistoryAdapter.kt) & [PdfHistoryActivity.kt](file:///C:/Users/Aaditya Shukla/StudioProjects/MediaShrinker/app/src/main/java/com/aaditya/mediashrinker/PdfHistoryActivity.kt)
- Apply the `doesUriExist` check to the Share button. Show `ErrorHelper.showFileMissingDialog` if the file is gone.

## Verification Plan
- **Cleaner**: Run Magic Cleaner and verify it finds similar photos.
- **Scroll Hint**: Verify it reappears on scroll-up and scrolls down when clicked.
- **Share**: Verify the "File Missing" dialog appears when trying to share a deleted file.
