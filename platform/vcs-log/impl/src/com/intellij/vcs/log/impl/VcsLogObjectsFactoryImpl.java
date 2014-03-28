package com.intellij.vcs.log.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcs.log.*;
import com.intellij.vcs.log.data.VcsLogDataHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * @author Kirill Likhodedov
 */
public class VcsLogObjectsFactoryImpl implements VcsLogObjectsFactory {

  private static final Logger LOG = Logger.getInstance(VcsLogObjectsFactoryImpl.class);

  @NotNull private final VcsLogManager myLogManager;

  public VcsLogObjectsFactoryImpl(@NotNull VcsLogManager logManager) {
    myLogManager = logManager;
  }

  @NotNull
  @Override
  public Hash createHash(@NotNull String stringHash) {
    return HashImpl.build(stringHash);
  }

  @NotNull
  @Override
  public VcsCommit createCommit(@NotNull Hash hash, @NotNull List<Hash> parents) {
    return new VcsCommitImpl(hash, parents);
  }

  @NotNull
  @Override
  public TimedVcsCommit createTimedCommit(@NotNull Hash hash, @NotNull List<Hash> parents, long timeStamp) {
    return new TimedVcsCommitImpl(hash, parents, timeStamp);
  }

  @NotNull
  @Override
  public VcsShortCommitDetails createShortDetails(@NotNull Hash hash, @NotNull List<Hash> parents, long timeStamp,
                                                  @NotNull VirtualFile root, @NotNull String subject,
                                                  @NotNull String authorName, String authorEmail) {
    VcsUser author = createUser(authorName, authorEmail);
    return new VcsShortCommitDetailsImpl(hash, parents, timeStamp, root, subject, author);
  }

  @NotNull
  @Override
  public VcsCommitMetadata createCommitMetadata(@NotNull Hash hash, @NotNull List<Hash> parents, long time, @NotNull VirtualFile root,
                                                @NotNull String subject, @NotNull String authorName, @NotNull String authorEmail,
                                                @NotNull String message, @NotNull String committerName,
                                                @NotNull String committerEmail, long authorTime) {
    VcsUser author = createUser(authorName, authorEmail);
    VcsUser committer = createUser(committerName, committerEmail);
    return new VcsCommitMetadataImpl(hash, parents, time, root, subject, author, message, committer, authorTime);
  }

  @NotNull
  @Override
  public VcsFullCommitDetails createFullDetails(@NotNull Hash hash, @NotNull List<Hash> parents, long time, VirtualFile root,
                                                @NotNull String subject, @NotNull String authorName, @NotNull String authorEmail,
                                                @NotNull String message, @NotNull String committerName, @NotNull String committerEmail,
                                                long authorTime,
                                                @NotNull ThrowableComputable<Collection<Change>, ? extends Exception> changesGetter) {
    VcsUser author = createUser(authorName, authorEmail);
    VcsUser committer = createUser(committerName, committerEmail);
    return new VcsChangesLazilyParsedDetails(hash, parents, time, root, subject, author, message, committer, authorTime, changesGetter);
  }

  @NotNull
  @Override
  public VcsUser createUser(@NotNull String name, @NotNull String email) {
    VcsLogDataHolder dataHolder = myLogManager.getDataHolder();
    if (dataHolder == null) {
      return new VcsUserImpl(name, email);
    }
    return dataHolder.getUserRegistry().createUser(name, email);
  }

  @NotNull
  @Override
  public VcsRef createRef(@NotNull Hash commitHash, @NotNull String name, @NotNull VcsRefType type, @NotNull VirtualFile root) {
    return new VcsRefImpl(commitHash, name, type, root);
  }

}
