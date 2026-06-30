package com.tlmpersonal.tlmpersonaldimension;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager;
import com.tlmpersonal.tlmpersonaldimension.item.DomainExpansionBauble;

@LittleMaidExtension
public class PersonalDimensionMaidPlugin implements ILittleMaid {
    @Override
    public void bindMaidBauble(BaubleManager manager) {
        manager.bind(Touhoulittlemaidpersonaldimension.DOMAIN_EXPANSION_BAUBLE, new DomainExpansionBauble());
        manager.bind(Touhoulittlemaidpersonaldimension.CHERRY_DOMAIN_BAUBLE, new com.tlmpersonal.tlmpersonaldimension.item.CherryDomainBauble());
        manager.bind(Touhoulittlemaidpersonaldimension.CAT_FAMILIAR_BAUBLE, new com.tlmpersonal.tlmpersonaldimension.item.CatFamiliarBauble());
        manager.bind(Touhoulittlemaidpersonaldimension.TETHERED_TELEPORT_BAUBLE, new com.tlmpersonal.tlmpersonaldimension.item.TetheredTeleportBauble());
    }
}
