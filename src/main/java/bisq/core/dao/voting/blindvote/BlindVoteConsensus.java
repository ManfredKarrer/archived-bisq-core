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

package bisq.core.dao.voting.blindvote;

import bisq.core.dao.state.StateService;
import bisq.core.dao.state.blockchain.OpReturnType;
import bisq.core.dao.voting.ballot.Ballot;
import bisq.core.dao.voting.ballot.BallotList;
import bisq.core.dao.voting.ballot.BallotListService;
import bisq.core.dao.voting.merit.MeritList;
import bisq.core.dao.voting.proposal.ProposalValidator;
import bisq.core.dao.voting.proposal.param.Param;

import bisq.common.app.Version;
import bisq.common.crypto.CryptoException;
import bisq.common.crypto.Encryption;
import bisq.common.crypto.Hash;
import bisq.common.util.Utilities;

import org.bitcoinj.core.Coin;

import javax.crypto.SecretKey;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * All consensus critical aspects are handled here.
 */
@Slf4j
public class BlindVoteConsensus {

    public static BallotList getSortedBallotList(BallotListService ballotListService, ProposalValidator proposalValidator) {
        final List<Ballot> ballotList = ballotListService.getBallotList().stream()
                .filter(ballot -> proposalValidator.isValidAndConfirmed(ballot.getProposal()))
                .distinct()
                .sorted(Comparator.comparing(Ballot::getProposalTxId)).collect(Collectors.toList());
        log.info("Sorted ballotList: " + ballotList);
        return new BallotList(ballotList);
    }

    public static MyBlindVoteList getSortedBlindVoteListOfCycle(BlindVoteService blindVoteService, BlindVoteValidator blindVoteValidator) {
        final List<BlindVote> list = blindVoteService.getVerifiedBlindVotes().stream()
                .filter(blindVoteValidator::isValidAndConfirmed) // prob. not needed
                .distinct()
                .sorted(Comparator.comparing(BlindVote::getTxId))
                .collect(Collectors.toList());

        log.info("Sorted blindVote txId list: " + list.stream()
                .map(BlindVote::getTxId)
                .collect(Collectors.toList()));
        return new MyBlindVoteList(list);
    }

    // 128 bit AES key is good enough for our use case
    public static SecretKey getSecretKey() {
        return Encryption.generateSecretKey(128);
    }

    // TODO should we use PB as data input or a more common encoding like json?
    // Are there risks that PB encoding format changes?
    public static byte[] getEncryptedVotes(VoteWithProposalTxIdList voteWithProposalTxIdList, SecretKey secretKey) throws CryptoException {
        final byte[] bytes = voteWithProposalTxIdList.toProtoMessage().toByteArray();
        final byte[] encrypted = Encryption.encrypt(bytes, secretKey);
        log.info("encrypted: " + Utilities.bytesAsHexString(encrypted));
        return encrypted;
    }

    public static byte[] getEncryptedMeritList(MeritList meritList, SecretKey secretKey) throws CryptoException {
        final byte[] bytes = meritList.toProtoMessage().toByteArray();
        final byte[] encrypted = Encryption.encrypt(bytes, secretKey);
        return encrypted;
    }

    public static byte[] getHashOfEncryptedProposalList(byte[] encryptedProposalList) {
        return Hash.getSha256Ripemd160hash(encryptedProposalList);
    }

    public static byte[] getOpReturnData(byte[] hash) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            outputStream.write(OpReturnType.BLIND_VOTE.getType());
            outputStream.write(Version.BLIND_VOTE_VERSION);
            outputStream.write(hash);
            final byte[] bytes = outputStream.toByteArray();
            log.info("OpReturnData: " + Utilities.bytesAsHexString(bytes));
            return bytes;
        } catch (IOException e) {
            // Not expected to happen ever
            e.printStackTrace();
            log.error(e.toString());
            throw e;
        }
    }

    public static Coin getFee(StateService stateService, int chainHeadHeight) {
        final Coin fee = Coin.valueOf(stateService.getParamValue(Param.BLIND_VOTE_FEE, chainHeadHeight));
        log.info("Fee for blind vote: " + fee);
        return fee;
    }
}