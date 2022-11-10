# How-to's

This document lists the how-to's around developing and maintaining jpss-jade-ccm.

## How to list local tags

```
git tags -l
```

## How to list remote tags

```
git ls-remote --tags origin
```

Example output:

```
debacff0e19beeac204dea8f851042f72a64248c        refs/tags/s13
7646876dbe1db4cfcb79323826c31d64ebd4ea5f        refs/tags/s14
31c76cb1c4b2eda38ac609190ce00c51ea11a786        refs/tags/s15
1e6107eb840fadf74a638507732f2f3ed8e4ccee        refs/tags/s16
7979c6d1f098eb2844be898f91675264ef521f1f        refs/tags/s17
bb781d5901b8d5ad6be1000f0e5e206bb98e6284        refs/tags/s17.1
a55e649ac4b49d0e9b0799c43a324c09d1c2c61e        refs/tags/s17.2
f6747e9c6265e25cb7cc14541e241e3567dcffaa        refs/tags/s17.3
7979c6d1f098eb2844be898f91675264ef521f1f        refs/tags/s17.4
c1a583cdcd032bd9af6a3d6db94f9fc7ce717f21        refs/tags/s18
483db44bfecc3a25e7f02d757822cac8e0745af9        refs/tags/s18.3
```

## How to delete a remote tag

git push origin :/refs/tags/<tag>

Example:

```
git push origin :refs/tags/s16.3
```

## How to name a release tag

s<sprint #>

E.g.,

s18 for sprint 18 release.

## How to name a build tag

s<sprint #>.<build #>

E.g.,

s18.1 for sprint 18 release, build 1.

The final build for a given release will not have a build number.

E.g.,

s18.3 -> s18

## How to clean up remote build tags at the end of a sprint

1. List all remote tags associated with a release

E.g.,

```
jpss-jade-ccm % git ls-remote --tags origin | grep s17
```

Example output
```
7979c6d1f098eb2844be898f91675264ef521f1f        refs/tags/s17
bb781d5901b8d5ad6be1000f0e5e206bb98e6284        refs/tags/s17.1
a55e649ac4b49d0e9b0799c43a324c09d1c2c61e        refs/tags/s17.2
f6747e9c6265e25cb7cc14541e241e3567dcffaa        refs/tags/s17.3
7979c6d1f098eb2844be898f91675264ef521f1f        refs/tags/s17.4
```

2. Delete each build tag

e.g.,

```
git push origin :refs/tags/s17.1
```

Example output
```
To https://github.com/bcgov/jpss-jade-ccm.git
 - [deleted]         s17.1
```
