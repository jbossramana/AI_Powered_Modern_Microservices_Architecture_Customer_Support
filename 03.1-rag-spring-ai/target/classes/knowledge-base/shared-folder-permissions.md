# Shared Folders and Permissions

## Sharing a folder

Right-click any folder → Share → enter the email addresses of people to invite. You can
set each person's role:

- **Viewer**: can view and download files, cannot upload, edit, or delete.
- **Editor**: can upload, edit, and delete files, cannot change sharing settings or
  remove other members.
- **Manager**: full control, including inviting/removing other members and deleting the
  folder. Only available on Pro and Business plans.

Free-plan accounts can share folders with up to 3 people as Viewers only; Pro allows up
to 25 people with any role; Business allows unlimited people and supports company-wide
shared drives.

## "I can't access a shared folder" — common causes

1. **The invite was sent to a different email than the one you signed in with.** Ask the
   folder owner which address they invited, and check Settings → Account for all email
   addresses linked to your account.
2. **The owner's account is over its storage limit and is read-only.** Existing shared
   files remain viewable, but the owner can't add new ones until they're back under
   their limit.
3. **A workspace administrator revoked external sharing.** Business plan admins can
   restrict sharing to only members of the same workspace under Admin Console → Sharing
   Policy. If this is enabled, outside collaborators lose access until an admin changes
   the policy or adds them to the workspace.
4. **The folder was moved into a different parent folder the person doesn't have access
   to.** Permissions in Acme Cloud Storage are inherited from the immediate parent folder
   only, not from the whole path - moving a subfolder can unexpectedly change who can see it.

## Permission changes and propagation

Permission changes usually take effect within a few seconds but can take up to 5 minutes
to propagate to all of a person's signed-in devices. If access still doesn't work after
5 minutes, the fix is almost always one of the four causes above, not a system delay.
