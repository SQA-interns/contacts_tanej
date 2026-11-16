# Page snapshot

```yaml
- generic [ref=e3]:
  - banner [ref=e4]:
    - generic [ref=e5]:
      - link "Contact Groups" [ref=e6] [cursor=pointer]:
        - /url: /
        - text: Contact Groups
      - navigation "Menu" [ref=e8]:
        - link "Groups" [ref=e9] [cursor=pointer]:
          - /url: /
        - link "Deleted groups" [ref=e10] [cursor=pointer]:
          - /url: /deleted-groups
        - link "Account" [ref=e11] [cursor=pointer]:
          - /url: /account
      - group "Language" [ref=e12]:
        - button "SL" [ref=e13]
        - button "EN" [pressed] [ref=e14]
  - main [ref=e15]:
    - generic [ref=e16]:
      - heading "Welcome" [level=1] [ref=e17]
      - paragraph [ref=e18]: Sign in to see your groups.
  - contentinfo [ref=e19]:
    - generic [ref=e20]: Contact Groups
```