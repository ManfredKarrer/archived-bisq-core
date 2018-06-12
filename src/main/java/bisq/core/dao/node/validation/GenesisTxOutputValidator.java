/*
 * This file is part of Bisq.
 *
 * bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.core.dao.node.validation;

import bisq.core.dao.state.StateService;
import bisq.core.dao.state.blockchain.TxOutput;
import bisq.core.dao.state.blockchain.TxOutputType;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

/**
 * Checks if an output is a BSQ output and apply state change.
 */
@Slf4j
public class GenesisTxOutputValidator extends TxOutputValidator {
    @Inject
    public GenesisTxOutputValidator(StateService stateService, OpReturnProcessor opReturnProcessor) {
        super(stateService, opReturnProcessor);
    }

    void validate(TxOutput txOutput, TxState txState) {
        long value = txOutput.getPaddedValue(stateService, opReturnProcessor);
        if (value <= txState.getAvailableInputValue()) {
            txState.subtractFromInputValue(value);
            applyStateChangeForBsqOutput(txOutput, TxOutputType.GENESIS_OUTPUT);
        } else {
            // If we get one output which is not funded sufficiently by the available
            // input value, we consider all remaining outputs as BTC outputs.
            // To achieve that we set the value to 0, so we avoid that following smaller
            // outputs might get interpreted as BSQ outputs.
            // In fact that cannot happen as the genesis tx is constructed carefully, so
            // it matches exactly the outputs.
            txState.setAvailableInputValue(0);

            applyStateChangeForBtcOutput(txOutput);
            log.warn("BTC output remains in genesis tx.");
        }
    }
}
