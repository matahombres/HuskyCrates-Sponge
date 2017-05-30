package pw.codehusky.huskycrates;

import com.google.inject.Inject;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.ArmorStand;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.event.world.chunk.LoadChunkEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import pw.codehusky.huskycrates.commands.Chest;
import pw.codehusky.huskycrates.commands.Crate;
import pw.codehusky.huskycrates.commands.Key;
import pw.codehusky.huskycrates.commands.elements.CrateElement;
import pw.codehusky.huskycrates.crate.CrateUtilities;
import pw.codehusky.huskycrates.crate.VirtualCrate;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by lokio on 12/28/2016.
 */
@SuppressWarnings("deprecation")
@Plugin(id="huskycrates", name = "HuskyCrates", version = "1.1.1", description = "A CratesReloaded Replacement for Sponge? lol")
public class HuskyCrates {
    //@Inject
    public Logger logger;


    @Inject
    private PluginContainer pC;
    @Inject
    @DefaultConfig(sharedRoot = false)
    public ConfigurationLoader<CommentedConfigurationNode> crateConfig;

    public Cause genericCause;
    public Scheduler scheduler;
    public CrateUtilities crateUtilities = new CrateUtilities(this);
    public String huskyCrateIdentifier = "☼1☼2☼3HUSKYCRATE-";
    public String armorStandIdentifier = "ABABABAB-CDDE-0000-8374-CAAAECAAAECA";
    public static HuskyCrates instance;
    @Listener
    public void gameInit(GamePreInitializationEvent event){
        logger = LoggerFactory.getLogger(pC.getName());
        logger.info("Let's not init VCrates here anymore. ://)");
        instance = this;
    }
    @Listener
    public void gameStarted(GameStartedServerEvent event){



        CommandSpec key = CommandSpec.builder()
                .description(Text.of("Get a key for a specified crate."))
                .arguments(
                        new CrateElement(Text.of("type")),
                        GenericArguments.playerOrSource(Text.of("player")),
                        GenericArguments.optional(GenericArguments.integer(Text.of("quantity")))
                )
                .permission("huskycrates.key")
                .executor(new Key())
                .build();


        CommandSpec chest = CommandSpec.builder()
                .description(Text.of("Get the placeable crate item."))
                .permission("huskycrates.chest")
                .arguments(
                        new CrateElement(Text.of("type")),
                        GenericArguments.playerOrSource(Text.of("player")),
                        GenericArguments.optional(GenericArguments.integer(Text.of("quantity")))
                ).executor(new Chest())
                .build();

        CommandSpec crateSpec = CommandSpec.builder()
                .description(Text.of("Main crates command"))
                .permission("huskycrates")
                .child(key, "key")
                .child(chest, "chest")
                .arguments(GenericArguments.optional(GenericArguments.remainingRawJoinedStrings(Text.of(""))))
                .executor(new Crate(this))
                .build();

        scheduler = Sponge.getScheduler();
        genericCause = Cause.of(NamedCause.of("PluginContainer",pC));
        Sponge.getCommandManager().register(this, crateSpec, "crate");
        if(!crateUtilities.hasInitalizedVirtualCrates){
            crateUtilities.generateVirtualCrates(crateConfig);
        }
        logger.info("Crates has been started.");
    }

    @Listener(order = Order.POST)
    public void postGameStart(GameStartedServerEvent event){
        Sponge.getScheduler().createTaskBuilder().async().execute(new Consumer<Task>() {
            @Override
            public void accept(Task task) {
                try {
                    JSONObject obj = JsonReader.readJsonFromUrl("https://api.github.com/repos/codehusky/HuskyCrates-Sponge/releases");
                    String[] thisVersion = pC.getVersion().get().split("\\.");
                    String[] remoteVersion = obj.getJSONArray("releases").getJSONObject(0).getString("tag_name").replace("v","").split("\\.");
                    for(int i = 0; i < Math.min(remoteVersion.length,thisVersion.length); i++){
                        if(!thisVersion[i].equals(remoteVersion[i])){
                            if(Integer.parseInt(thisVersion[i]) > Integer.parseInt(remoteVersion[i])){
                                //we're ahead
                                logger.warn("----------------------------------------------------");
                                logger.warn("Running unreleased version. (Developer build?)");
                                logger.warn("----------------------------------------------------");
                            }else{
                                //we're behind
                                logger.warn("----------------------------------------------------");
                                logger.warn("Your version of HuskyCrates is out of date!");
                                logger.warn("Your version: v" + pC.getVersion().get());
                                logger.warn("Latest version: " + obj.getJSONArray("releases").getJSONObject(0).getString("tag_name"));
                                logger.warn("Update here: https://goo.gl/hgtPMR");
                                logger.warn("----------------------------------------------------");
                            }
                            return;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).submit(this);
    }

    @Listener(order = Order.POST)
    public void chunkLoad(LoadChunkEvent event){
        if(!crateUtilities.hasInitalizedVirtualCrates){
            crateUtilities.generateVirtualCrates(crateConfig);
        }
        for(Entity e : event.getTargetChunk().getEntities()){
            if(e instanceof ArmorStand){
                crateUtilities.populatePhysicalCrates(event.getTargetChunk());
                return;
            }
        }
    }

    @Listener(order = Order.POST)
    public void worldLoaded(LoadWorldEvent event){
        if(!crateUtilities.hasInitalizedVirtualCrates){
            crateUtilities.generateVirtualCrates(crateConfig);
        }
        crateUtilities.populatePhysicalCrates(event.getTargetWorld());
    }
    @Listener
    public void gameReloaded(GameReloadEvent event){
        crateUtilities.generateVirtualCrates(crateConfig);
        for(World e: Sponge.getServer().getWorlds()){
            crateUtilities.populatePhysicalCrates(e);
        }
    }
    private boolean blockCanBeCrate(BlockType type){
        return type==BlockTypes.CHEST ||
                type==BlockTypes.TRAPPED_CHEST ||
                type==BlockTypes.ENDER_CHEST;
    }

    @Listener
    public void placeBlock(ChangeBlockEvent event){
        if(event instanceof ChangeBlockEvent.Place || event instanceof ChangeBlockEvent.Break) {
            BlockType t = event.getTransactions().get(0).getOriginal().getLocation().get().getBlock().getType();
            if (blockCanBeCrate(t)) {
                crateUtilities.recognizeChest(event.getTransactions().get(0).getOriginal().getLocation().get());
            }
        }
    }

    @Listener
    public void crateInteract(InteractBlockEvent.Secondary.MainHand event){
        /*Player pp = (Player) event.getCause().root();

        ItemStack ss = pp.getItemInHand(HandTypes.MAIN_HAND).get();
        pp.getInventory().offer(ItemStack.builder().fromContainer(ss.toContainer().set(DataQuery.of("UnsafeDamage"),3)).build());*/
        if(!event.getTargetBlock().getLocation().isPresent())
            return;

        Location<World> blk = event.getTargetBlock().getLocation().get();
        if(blk.getBlock().getType() == BlockTypes.CHEST) {
            Player plr = (Player)event.getCause().root();
            TileEntity te = blk.getTileEntity().get();
            Inventory inv = ((TileEntityCarrier) te).getInventory();
            String name = inv.getName().get();
            if(name.contains(huskyCrateIdentifier)){
                event.setCancelled(true);
                String crateType = name.replace(huskyCrateIdentifier, "");
                VirtualCrate vc = crateUtilities.getVirtualCrate(crateType);
                crateUtilities.recognizeChest(te.getLocation());
                if(plr.getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
                    ItemStack inhand = plr.getItemInHand(HandTypes.MAIN_HAND).get();
                    if(inhand.getItem() == vc.getKeyType() && inhand.get(Keys.ITEM_LORE).isPresent()) {
                        List<Text> lore = inhand.get(Keys.ITEM_LORE).get();
                        if(lore.size() > 1) {
                            String idline = lore.get(1).toPlain();
                            if(idline.contains("crate_")) {
                                if(idline.replace("crate_","").equalsIgnoreCase(crateType)) {
                                    if(!plr.hasPermission("huskycrates.tester")){
                                        if(inhand.getQuantity() == 1)
                                            plr.setItemInHand(HandTypes.MAIN_HAND,null);
                                        else{
                                            ItemStack tobe = inhand.copy();
                                            tobe.setQuantity(tobe.getQuantity()-1);
                                            plr.setItemInHand(HandTypes.MAIN_HAND,tobe);
                                        }
                                    }
                                    Task.Builder upcoming = scheduler.createTaskBuilder();

                                    upcoming.execute(() -> {
                                        crateUtilities.launchCrateForPlayer(crateType, plr, this);
                                    }).delayTicks(1).submit(this);
                                    return;
                                }
                            }
                        }
                    }

                }
                plr.playSound(SoundTypes.BLOCK_ANVIL_LAND,plr.getLocation().getPosition(),0.5);
                try {
                    plr.sendMessage(Text.of("You need a ", TextSerializers.FORMATTING_CODE.deserialize(vc.displayName + " Key"), " to open this crate."));
                }catch(Exception e){
                    plr.sendMessage(Text.of(TextColors.RED,"Critical crate failure, contact the administrator. (Admins, check console!)"));
                    e.printStackTrace();
                }
            }


        }
    }


    public CrateUtilities getCrateUtilities() {
        return crateUtilities;
    }

    public String getHuskyCrateIdentifier() {
        return huskyCrateIdentifier;
    }
}
