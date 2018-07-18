/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.core.dao;

import bisq.core.btc.exceptions.TransactionVerificationException;
import bisq.core.btc.exceptions.WalletException;
import bisq.core.dao.bonding.lockup.LockupService;
import bisq.core.dao.bonding.unlock.UnlockService;
import bisq.core.dao.state.BsqStateListener;
import bisq.core.dao.state.BsqStateService;
import bisq.core.dao.state.blockchain.Block;
import bisq.core.dao.state.blockchain.Tx;
import bisq.core.dao.state.blockchain.TxOutput;
import bisq.core.dao.state.blockchain.TxOutputKey;
import bisq.core.dao.state.blockchain.TxType;
import bisq.core.dao.state.ext.Param;
import bisq.core.dao.state.period.DaoPhase;
import bisq.core.dao.state.period.PeriodService;
import bisq.core.dao.voting.ValidationException;
import bisq.core.dao.voting.ballot.Ballot;
import bisq.core.dao.voting.ballot.BallotListService;
import bisq.core.dao.voting.ballot.FilteredBallotListService;
import bisq.core.dao.voting.ballot.vote.Vote;
import bisq.core.dao.voting.blindvote.MyBlindVoteListService;
import bisq.core.dao.voting.myvote.MyVote;
import bisq.core.dao.voting.myvote.MyVoteListService;
import bisq.core.dao.voting.proposal.FilteredProposalListService;
import bisq.core.dao.voting.proposal.MyProposalListService;
import bisq.core.dao.voting.proposal.Proposal;
import bisq.core.dao.voting.proposal.ProposalConsensus;
import bisq.core.dao.voting.proposal.ProposalWithTransaction;
import bisq.core.dao.voting.proposal.burnbond.BurnBondProposalService;
import bisq.core.dao.voting.proposal.compensation.CompensationProposalService;
import bisq.core.dao.voting.proposal.param.ChangeParamProposalService;

import bisq.common.handlers.ErrorMessageHandler;
import bisq.common.handlers.ExceptionHandler;
import bisq.common.handlers.ResultHandler;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;

import javax.inject.Inject;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import javafx.collections.ObservableList;

import java.io.IOException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Provides a facade to interact with the Dao domain. Hides complexity and domain details to clients (e.g. UI or APIs)
 * by providing a reduced API and/or aggregating subroutines.
 */
public class DaoFacade {
    private final FilteredProposalListService filteredProposalListService;
    private final BallotListService ballotListService;
    private final FilteredBallotListService filteredBallotListService;
    private final MyProposalListService myProposalListService;
    private final BsqStateService bsqStateService;
    private final PeriodService periodService;
    private final MyBlindVoteListService myBlindVoteListService;
    private final MyVoteListService myVoteListService;
    private final CompensationProposalService compensationProposalService;
    private final ChangeParamProposalService changeParamProposalService;
    private final BurnBondProposalService burnBondPRoposalService;
    private final LockupService lockupService;
    private final UnlockService unlockService;

    private final ObjectProperty<DaoPhase.Phase> phaseProperty = new SimpleObjectProperty<>(DaoPhase.Phase.UNDEFINED);

    @Inject
    public DaoFacade(MyProposalListService myProposalListService,
                     FilteredProposalListService filteredProposalListService,
                     BallotListService ballotListService,
                     FilteredBallotListService filteredBallotListService,
                     BsqStateService bsqStateService,
                     PeriodService periodService,
                     MyBlindVoteListService myBlindVoteListService,
                     MyVoteListService myVoteListService,
                     CompensationProposalService compensationProposalService,
                     ChangeParamProposalService changeParamProposalService,
                     BurnBondProposalService burnBondPRoposalService,
                     LockupService lockupService,
                     UnlockService unlockService) {
        this.filteredProposalListService = filteredProposalListService;
        this.ballotListService = ballotListService;
        this.filteredBallotListService = filteredBallotListService;
        this.myProposalListService = myProposalListService;
        this.bsqStateService = bsqStateService;
        this.periodService = periodService;
        this.myBlindVoteListService = myBlindVoteListService;
        this.myVoteListService = myVoteListService;
        this.compensationProposalService = compensationProposalService;
        this.changeParamProposalService = changeParamProposalService;
        this.burnBondPRoposalService = burnBondPRoposalService;
        this.lockupService = lockupService;
        this.unlockService = unlockService;

        bsqStateService.addBsqStateListener(new BsqStateListener() {
            @Override
            public void onNewBlockHeight(int blockHeight) {
                if (blockHeight > 0 && periodService.getCurrentCycle() != null)
                    periodService.getCurrentCycle().getPhaseForHeight(blockHeight).ifPresent(phaseProperty::set);
            }

            @Override
            public void onEmptyBlockAdded(Block block) {
            }

            @Override
            public void onParseTxsComplete(Block block) {
            }

            @Override
            public void onParseBlockChainComplete() {
            }
        });
    }

    public void addBsqStateListener(BsqStateListener listener) {
        bsqStateService.addBsqStateListener(listener);
    }

    public void removeBsqStateListener(BsqStateListener listener) {
        bsqStateService.removeBsqStateListener(listener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    //
    // Phase: Proposal
    //
    ///////////////////////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Use case: Present lists
    ///////////////////////////////////////////////////////////////////////////////////////////

    public ObservableList<Proposal> getActiveOrMyUnconfirmedProposals() {
        return filteredProposalListService.getActiveOrMyUnconfirmedProposals();
    }

    public ObservableList<Proposal> getClosedProposals() {
        return filteredProposalListService.getClosedProposals();
    }

    public List<Proposal> getMyProposals() {
        return myProposalListService.getList();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Use case: Create proposal
    ///////////////////////////////////////////////////////////////////////////////////////////

    // Creation of Proposal and proposalTransaction
    public ProposalWithTransaction getCompensationProposalWithTransaction(String name,
                                                                          String title,
                                                                          String description,
                                                                          String link,
                                                                          Coin requestedBsq,
                                                                          String bsqAddress)
            throws ValidationException, InsufficientMoneyException, IOException, TransactionVerificationException,
            WalletException {
        return compensationProposalService.createProposalWithTransaction(name,
                title,
                description,
                link,
                requestedBsq,
                bsqAddress);
    }

    public ProposalWithTransaction getParamProposalWithTransaction(String name,
                                                                   String title,
                                                                   String description,
                                                                   String link,
                                                                   Param param,
                                                                   long paramValue)
            throws ValidationException, InsufficientMoneyException, IOException, TransactionVerificationException,
            WalletException {
        return changeParamProposalService.createProposalWithTransaction(name,
                title,
                description,
                link,
                param,
                paramValue);
    }

    public ProposalWithTransaction getBurnBondProposalWithTransaction(String name,
                                                                      String title,
                                                                      String description,
                                                                      String link,
                                                                      String bondId)
            throws ValidationException, InsufficientMoneyException, IOException, TransactionVerificationException,
            WalletException {
        return burnBondPRoposalService.createProposalWithTransaction(name,
                title,
                description,
                link,
                bondId);
    }

    // Show fee
    public Coin getProposalFee(int chainHeight) {
        return ProposalConsensus.getFee(bsqStateService, chainHeight);
    }

    // Publish proposal tx, proposal payload and and persist it to myProposalList
    public void publishMyProposal(Proposal proposal, Transaction transaction, ResultHandler resultHandler,
                                  ErrorMessageHandler errorMessageHandler) {
        myProposalListService.publishTxAndPayload(proposal, transaction, resultHandler, errorMessageHandler);
    }

    // Check if it is my proposal
    public boolean isMyProposal(Proposal proposal) {
        return myProposalListService.isMine(proposal);
    }

    // Remove my proposal
    public boolean removeMyProposal(Proposal proposal) {
        return myProposalListService.remove(proposal);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    //
    // Phase: Blind Vote
    //
    ///////////////////////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Use case: Present lists
    ///////////////////////////////////////////////////////////////////////////////////////////

    public ObservableList<Ballot> getValidAndConfirmedBallots() {
        return filteredBallotListService.getValidAndConfirmedBallots();
    }

    public ObservableList<Ballot> getClosedBallots() {
        return filteredBallotListService.getClosedBallots();
    }

    public List<MyVote> getMyVoteList() {
        return myVoteListService.getMyVoteList().getList();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Use case: Vote
    ///////////////////////////////////////////////////////////////////////////////////////////

    // Vote on ballot
    public void setVote(Ballot ballot, @Nullable Vote vote) {
        ballotListService.setVote(ballot, vote);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Use case: Create blindVote
    ///////////////////////////////////////////////////////////////////////////////////////////

    // When creating blind vote we present fee
    public Coin getBlindVoteFeeForCycle() {
        return myBlindVoteListService.getBlindVoteFee();
    }

    // Used for mining fee estimation
    public Transaction getDummyBlindVoteTx(Coin stake, Coin blindVoteFee) throws WalletException, InsufficientMoneyException, TransactionVerificationException {
        return myBlindVoteListService.getDummyBlindVoteTx(stake, blindVoteFee);
    }

    // Publish blindVote tx and broadcast blindVote to p2p network and store to blindVoteList.
    public void publishBlindVote(Coin stake, ResultHandler resultHandler, ExceptionHandler exceptionHandler) {
        myBlindVoteListService.publishBlindVote(stake, resultHandler, exceptionHandler);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    //
    // Generic
    //
    ///////////////////////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Use case: Presentation of phases
    ///////////////////////////////////////////////////////////////////////////////////////////

    public int getFirstBlockOfPhase(int height, DaoPhase.Phase phase) {
        return periodService.getFirstBlockOfPhase(height, phase);
    }

    public int getLastBlockOfPhase(int height, DaoPhase.Phase phase) {
        return periodService.getLastBlockOfPhase(height, phase);
    }

    public int getDurationForPhase(DaoPhase.Phase phase) {
        return periodService.getDurationForPhase(phase, bsqStateService.getChainHeight());
    }

    // listeners for phase change
    public ReadOnlyObjectProperty<DaoPhase.Phase> phaseProperty() {
        return phaseProperty;
    }

    public int getChainHeight() {
        return bsqStateService.getChainHeight();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Use case: Bonding
    ///////////////////////////////////////////////////////////////////////////////////////////

    //TODO maybe merge lockupService and unlockService as bondService?
    // Publish lockup tx
    public void publishLockupTx(Coin lockupAmount, int lockTime, ResultHandler resultHandler,
                                ExceptionHandler exceptionHandler) {
        lockupService.publishLockupTx(lockupAmount, lockTime, resultHandler, exceptionHandler);
    }

    // Publish unlock tx
    public void publishUnlockTx(String lockupTxId, ResultHandler resultHandler,
                                ExceptionHandler exceptionHandler) {
        unlockService.publishUnlockTx(lockupTxId, resultHandler, exceptionHandler);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Use case: Present transaction related state
    ///////////////////////////////////////////////////////////////////////////////////////////

    public Optional<Tx> getTx(String txId) {
        return bsqStateService.getTx(txId);
    }

    public Set<TxOutput> getUnspentBlindVoteStakeTxOutputs() {
        return bsqStateService.getUnspentBlindVoteStakeTxOutputs();
    }

    public int getGenesisBlockHeight() {
        return bsqStateService.getGenesisBlockHeight();
    }

    public String getGenesisTxId() {
        return bsqStateService.getGenesisTxId();
    }

    public Coin getGenesisTotalSupply() {
        return bsqStateService.getGenesisTotalSupply();
    }

    public Set<Tx> getFeeTxs() {
        return bsqStateService.getBurntFeeTxs();
    }

    public Set<TxOutput> getUnspentTxOutputs() {
        return bsqStateService.getUnspentTxOutputs();
    }

    public Set<Tx> getTxs() {
        return bsqStateService.getTxs();
    }

    public Optional<TxOutput> getLockupTxOutput(String txId) {
        return bsqStateService.getLockupTxOutput(txId);
    }

    public long getTotalBurntFee() {
        return bsqStateService.getTotalBurntFee();
    }

    public long getTotalLockupAmount() {
        return bsqStateService.getTotalLockupAmount();
    }

    public long getTotalAmountOfUnLockingTxOutputs() {
        return bsqStateService.getTotalAmountOfUnLockingTxOutputs();
    }

    public long getTotalAmountOfUnLockedTxOutputs() {
        return bsqStateService.getTotalAmountOfUnLockedTxOutputs();
    }

    public long getTotalIssuedAmountFromCompRequests() {
        return bsqStateService.getTotalIssuedAmount();
    }

    public long getBlockTime(int issuanceBlockHeight) {
        return bsqStateService.getBlockTime(issuanceBlockHeight);
    }

    public int getIssuanceBlockHeight(String txId) {
        return bsqStateService.getIssuanceBlockHeight(txId);
    }

    public Optional<Integer> getLockTime(String txId) {
        return bsqStateService.getLockTime(txId);
    }

    public boolean isIssuanceTx(String txId) {
        return bsqStateService.isIssuanceTx(txId);
    }

    public boolean hasTxBurntFee(String hashAsString) {
        return bsqStateService.hasTxBurntFee(hashAsString);
    }

    public Optional<TxType> getOptionalTxType(String txId) {
        return bsqStateService.getOptionalTxType(txId);
    }

    public TxType getTxType(String txId) {
        return bsqStateService.getTx(txId).map(Tx::getTxType).orElse(TxType.UNDEFINED_TX_TYPE);
    }

    public boolean isInPhaseButNotLastBlock(DaoPhase.Phase phase) {
        return periodService.isInPhaseButNotLastBlock(phase);
    }

    public boolean isUnspent(TxOutputKey key) {
        return bsqStateService.isUnspent(key);
    }

}
