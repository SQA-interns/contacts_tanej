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
      - heading "Not found" [level=1] [ref=e17]
      - paragraph [ref=e18]: This page does not exist or you have no access to it.
      - link "Back to groups" [ref=e19] [cursor=pointer]:
        - /url: /
  - contentinfo [ref=e20]:
    - generic [ref=e21]: Contact Groups
```