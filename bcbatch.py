"""

bcbatch.py

Run a batch of battlecode games, to test two or more players
on a varied set of maps, for many games.

TASKS TO COMPLETE:
[x] RunnerInfo.get_all_players
[x] RunnerInfo.get_all_maps
[x] arg_parser in main
[x] RunnerInfo.__init__
[x] GameResult
[x] run_game
[ ] better print_results
[ ] use gradle daemon
"""

__author__ = "Arya Kumar"
__email__ = "thearyaskumar[at]icloud.com"
__date__ = "01/05/2020"


import argparse
import collections
import multiprocessing
import subprocess
import time


class RunnerInfo:
    def __init__(self, parser):
        ALL_PLAYERS = self.get_all_players()
        ALL_MAPS = self.get_all_maps()

        args = parser.parse_args()
        if not (args.players or args.all_players):
            parser.error("Must specify one of --players or --all-players")

        if not (args.maps or args.all_maps):
            parser.error("Must specify one of --maps or --all-maps")

        if args.host:  # host style:
            self.player_ones = [args.host]
        else:  # round robin style
            self.player_ones = args.players or ALL_PLAYERS

        self.player_twos = args.players or ALL_PLAYERS
        self.maps = args.maps or ALL_MAPS

    def get_all_players(self):
        """ Returns every player in the src/ folder """
        rawlines = subprocess.check_output("./gradlew listPlayers", shell=True).decode()
        return [l[8:].strip() for l in rawlines.split("\n") if l.startswith("PLAYER: ")]

    def get_all_maps(self):
        """ Returns all the maps in the maps/ folder """
        rawlines = subprocess.check_output("./gradlew listMaps", shell=True).decode()
        return [l[5:].strip() for l in rawlines.split("\n") if l.startswith("MAP: ")]

    def matchings(self):
        """ Generator that returns the correct number of each m
            atching of p1 and p2 """
        for p1 in self.player_ones:
            for p2 in self.player_twos:
                # if p1 == p2:
                #     continue  # disallow same player games
                for m in self.maps:
                    yield p1, p2, m


GameResult = collections.namedtuple("GameResult", "p1 p2 map winner reason replay_loc")


def print_results(game_results):
    """ Given a list of GameResult objects prints the overall 
        results for every possible pairing. """
    print("\n---RESULTS---")
    for result in game_results:
        print(
            "{}{} vs {}{} on {}".format(
                result.p1,
                "(WINNER)" if result.winner == "A" else "",
                result.p2,
                "(WINNER)" if result.winner == "B" else "",
                result.map,
            )
        )


def run_game(p1, p2, m):
    """ run a single game between p1 and p2 on map m, and returns 
        a GameResult object. """
    print("running {} vs {} on map {}".format(p1, p2, m))
    saveloc = "matches/{}-vs-{}-on-{}-@{}".format(p1, p2, m, int(time.time()))
    rawlines = subprocess.check_output(
        "./gradlew run -PteamA={} -PteamB={} -Pmaps={} -Preplay={}".format(
            p1, p2, m, saveloc
        ),
        shell=True,
    ).decode()

    lines = rawlines.splitlines()
    endingline = lines.index(
        "[server] -------------------- Match Finished --------------------"
    )

    winner = "A" if "(A)" in lines[endingline - 2] else "B"
    reason = "\n".join(lines[endingline - 2 : endingline])
    return GameResult(p1, p2, m, winner, reason, saveloc)


def run_games(runner_info):
    """ Runs all games, as specified in the runner_info object. """
    pool = multiprocessing.Pool(multiprocessing.cpu_count())
    return pool.starmap(run_game, runner_info.matchings())


def build_argparser():
    """ Builds and returns an ArgumentParser object"""
    arg_parser = argparse.ArgumentParser()

    arg_parser.add_argument(
        "--all-maps",
        help="Runs all teams specified on all maps in maps/",
        action="store_true",
    )
    arg_parser.add_argument(
        "--all-players",
        help="Runs all teams in src/ on the specified maps",
        action="store_true",
    )
    arg_parser.add_argument(
        "--host",
        help="Specify a host for the tournament if playing one player against many",
        action="store",
    )

    arg_parser.add_argument(
        "--players", help="specify players for a round robin tournament", nargs="+"
    )
    arg_parser.add_argument(
        "--maps", help="specify maps for a round robin tournament", nargs="+"
    )
    # arg_parser.add_argument("--watch",help="Watch the matches in the visualizer",action="store_true",)
    return arg_parser


def main():
    arg_parser = build_argparser()
    runner_info = RunnerInfo(arg_parser)
    game_results = run_games(runner_info)
    print_results(game_results)


if __name__ == "__main__":
    main()
