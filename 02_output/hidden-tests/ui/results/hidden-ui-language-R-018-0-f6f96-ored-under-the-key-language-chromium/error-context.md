# Page snapshot

```yaml
- generic [ref=e3]:
  - banner [ref=e4]:
    - generic [ref=e5]:
      - link "Skupine kontaktov" [ref=e6] [cursor=pointer]:
        - /url: /
        - text: Skupine kontaktov
      - navigation "Meni" [ref=e8]:
        - link "Skupine" [ref=e9] [cursor=pointer]:
          - /url: /
        - link "Izbrisane skupine" [ref=e10] [cursor=pointer]:
          - /url: /deleted-groups
        - link "Račun" [ref=e11] [cursor=pointer]:
          - /url: /account
      - group "Jezik" [ref=e12]:
        - button "SL" [pressed] [ref=e13]
        - button "EN" [ref=e14]
  - main [ref=e15]:
    - generic [ref=e16]:
      - heading "Dobrodošli" [level=1] [ref=e17]
      - paragraph [ref=e18]: Prijavite se, da si ogledate svoje skupine.
  - contentinfo [ref=e19]:
    - generic [ref=e20]: Skupine kontaktov
```