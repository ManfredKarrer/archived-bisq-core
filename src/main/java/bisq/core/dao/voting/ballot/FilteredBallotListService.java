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

package bisq.core.dao.voting.ballot;

import bisq.core.dao.state.BlockListener;
import bisq.core.dao.state.ParseBlockChainListener;
import bisq.core.dao.state.StateService;
import bisq.core.dao.state.blockchain.Block;
import bisq.core.dao.state.period.PeriodService;
import bisq.core.dao.voting.proposal.ProposalValidator;

import com.google.inject.Inject;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.util.List;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Provides filtered observableLists of the ballots from BallotListService.
 */
@Slf4j
public class FilteredBallotListService implements ParseBlockChainListener, BallotListService.BallotListChangeListener, BlockListener {
    private final BallotListService ballotListService;

    @Getter
    private final ObservableList<Ballot> allBallots = FXCollections.observableArrayList();
    @Getter
    private final FilteredList<Ballot> validAndConfirmedBallots = new FilteredList<>(allBallots);
    @Getter
    private final FilteredList<Ballot> closedBallots = new FilteredList<>(allBallots);


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    public FilteredBallotListService(BallotListService ballotListService,
                                     PeriodService periodService,
                                     StateService stateService,
                                     ProposalValidator proposalValidator) {
        this.ballotListService = ballotListService;

        stateService.addParseBlockChainListener(this);
        stateService.addBlockListener(this);

        validAndConfirmedBallots.setPredicate(ballot -> {
            return proposalValidator.isValidAndConfirmed(ballot.getProposal());
        });

        closedBallots.setPredicate(ballot -> {
            final String proposalTxId = ballot.getProposalTxId();
            return stateService.getTx(proposalTxId).isPresent() &&
                    stateService.getTx(proposalTxId)
                            .filter(tx -> !periodService.isTxInCorrectCycle(tx.getBlockHeight(), stateService.getChainHeight()))
                            .isPresent();
        });

    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // ParseBlockChainListener
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onComplete() {
        ballotListService.addListener(this);
        onListChanged(ballotListService.getBallotList().getList());
    }

    @Override
    public void onBlockAdded(Block block) {
        onListChanged(ballotListService.getBallotList().getList());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // BallotListService.BallotListChangeListener
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onListChanged(List<Ballot> list) {
        allBallots.clear();
        allBallots.addAll(list);
    }
}