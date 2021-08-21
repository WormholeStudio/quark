package studio.wormhole.quark.command.mobius.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Equivalents {
    long create_at;
    BigInteger interest;
    long last_update_at;
    BigInteger token_amount;
    TokenCode token_code;
    BigInteger distribution_amount;
    BigInteger last_borrow_index;
}
